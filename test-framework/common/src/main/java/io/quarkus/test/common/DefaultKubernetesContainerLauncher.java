package io.quarkus.test.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.deployment.util.ContainerRuntimeUtil;

public class DefaultKubernetesContainerLauncher implements KubernetesContainerArtifactLauncher {

    private static final Logger log = Logger.getLogger(DefaultKubernetesContainerLauncher.class);

    private static final String TEST_RUN_LABEL = "io.quarkus.testing/run-id";

    private int httpPort;

    private Path manifestPath;
    private String deploymentTarget;
    private String containerImage;
    private String testRegistry;
    private Optional<String> testTag;
    private boolean insecureRegistry;
    private Optional<String> namespace;
    private String exposure;
    private OptionalInt externalPort;
    private boolean deleteAfterTest;
    private Duration waitTimeout;

    private final String testRunId = "quarkus-it-" + RandomStringUtils.insecure().next(8, true, false);

    private KubernetesClient client;
    private LocalPortForward portForward;
    private String previousHttpHost;
    private final List<HasMetadata> appliedResources = new ArrayList<>();

    @Override
    public void init(KubernetesInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.manifestPath = initContext.manifestPath();
        this.deploymentTarget = initContext.deploymentTarget();
        this.containerImage = initContext.containerImage();
        this.testRegistry = initContext.testRegistry();
        this.testTag = initContext.testTag();
        this.insecureRegistry = initContext.insecureRegistry();
        this.namespace = initContext.namespace();
        this.exposure = initContext.exposure();
        this.externalPort = initContext.externalPort();
        this.deleteAfterTest = initContext.deleteAfterTest();
        this.waitTimeout = initContext.waitTimeout();
    }

    @Override
    public ListeningAddresses start() throws IOException {
        try {
            String pushedImage = retagAndPushImage();

            client = buildClient();
            List<HasMetadata> resources = loadAndPatchManifest(pushedImage);

            applyAndWaitUntilReady(resources);

            Optional<ListeningAddress> address = "external".equals(exposure) ? exposeViaRouteOrIngress(resources)
                    : exposeViaPortForward(resources);

            address.ifPresent(this::waitForHttpReadiness);

            return new ListeningAddresses(address, Optional.empty());
        } catch (RuntimeException | IOException e) {
            close();
            throw e;
        }
    }

    private String retagAndPushImage() {
        ContainerRuntimeUtil.ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
        String binary = containerRuntime.getExecutableName();

        try {
            runProcess(binary, "image", "inspect", containerImage);
        } catch (RuntimeException e) {
            throw new IllegalStateException("The image '" + containerImage + "' was not found locally. " +
                    "Make sure the build produced and pushed a container image, e.g. by setting " +
                    "'quarkus.container-image.build=true' before running the integration test.", e);
        }

        String pushedImage = buildTestImageReference(containerImage, testRegistry, testTag, testRunId);

        log.infof("Retagging '%s' as '%s' and pushing it to the test registry", containerImage, pushedImage);
        runProcess(binary, "tag", containerImage, pushedImage);
        // --tls-verify is a Podman-specific flag (no Docker CLI equivalent - Docker relies on daemon-level
        // insecure-registries configuration instead), so this only has an effect when pushing with Podman.
        if (insecureRegistry && containerRuntime.isPodman()) {
            runProcess(binary, "push", "--tls-verify=false", pushedImage);
        } else {
            runProcess(binary, "push", pushedImage);
        }

        return pushedImage;
    }

    /**
     * Builds the reference of the image pushed to the test registry: the registry portion of the original image
     * is replaced with {@code testRegistry}, and the repository path (group/name) is kept. If {@code fixedTag} is
     * present it's used as-is (so repeated runs push to - and overwrite - the same tag, instead of accumulating a
     * new uniquely-tagged image in the registry on every run); otherwise a random suffix is appended to the
     * original tag so concurrent runs don't collide.
     */
    static String buildTestImageReference(String originalImage, String testRegistry, Optional<String> fixedTag,
            String uniqueSuffix) {
        int lastSlash = originalImage.lastIndexOf('/');
        int tagColonIndex = originalImage.indexOf(':', lastSlash + 1);
        String repository = tagColonIndex == -1 ? originalImage : originalImage.substring(0, tagColonIndex);
        String originalTag = tagColonIndex == -1 ? "latest" : originalImage.substring(tagColonIndex + 1);

        String repositoryPath = stripLeadingRegistryHost(repository);
        String tag = fixedTag.orElseGet(() -> originalTag + "-test-" + uniqueSuffix);

        return testRegistry + "/" + repositoryPath + ":" + tag;
    }

    private static String stripLeadingRegistryHost(String repository) {
        int firstSlash = repository.indexOf('/');
        if (firstSlash == -1) {
            return repository;
        }
        String firstSegment = repository.substring(0, firstSlash);
        boolean looksLikeHost = firstSegment.contains(".") || firstSegment.contains(":")
                || "localhost".equals(firstSegment);
        return looksLikeHost ? repository.substring(firstSlash + 1) : repository;
    }

    private List<HasMetadata> loadAndPatchManifest(String pushedImage) throws IOException {
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException(
                    ("Could not find generated %s manifest at %s. Make sure the 'quarkus-kubernetes' extension is "
                            + "present and 'quarkus.kubernetes.deployment-target' includes '%s'.")
                            .formatted(deploymentTarget, manifestPath.toAbsolutePath(), deploymentTarget));
        }
        String manifest = Files.readString(manifestPath);
        String patched = manifest.replace(containerImage, pushedImage);
        return client.load(new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8))).items();
    }

    private KubernetesClient buildClient() {
        Config config = Config.autoConfigure(null);
        namespace.ifPresent(config::setNamespace);
        // Not every kubeconfig context specifies a namespace (e.g. kind's generated one doesn't) - fall back to
        // "default" explicitly rather than leaving it null, which fabric8 rejects for any create/apply call.
        if (config.getNamespace() == null || config.getNamespace().isBlank()) {
            config.setNamespace("default");
        }
        KubernetesClient kubernetesClient = new KubernetesClientBuilder().withConfig(config).build();
        return "openshift".equals(deploymentTarget) ? kubernetesClient.adapt(OpenShiftClient.class) : kubernetesClient;
    }

    private void applyAndWaitUntilReady(List<HasMetadata> resources) {
        log.infof("Deploying to %s server: %s in namespace: %s.", deploymentTarget, client.getMasterUrl(),
                client.getNamespace());

        List<HasMetadata> readinessApplicable = new ArrayList<>();
        for (HasMetadata resource : resources) {
            labelForCleanup(resource);
            // Resource names are fixed (derived from the app name), not randomized per run - createOrReplace()
            // (matching `kubectl apply` semantics) avoids a 409 Conflict if a previous run's resources are still
            // around (e.g. it crashed before cleanup, or ran with delete-after-test=false).
            client.resource(resource).createOrReplace();
            appliedResources.add(resource);
            log.infof("Applied: %s %s.", resource.getKind(), resource.getMetadata().getName());
            if (Readiness.getInstance().isReadinessApplicable(resource)) {
                readinessApplicable.add(resource);
            }
        }

        for (HasMetadata resource : readinessApplicable) {
            log.infof("Waiting for %s %s to be ready...", resource.getKind(), resource.getMetadata().getName());
            client.resource(resource).waitUntilReady(waitTimeout.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void labelForCleanup(HasMetadata resource) {
        Map<String, String> labels = resource.getMetadata().getLabels();
        if (labels == null) {
            labels = new HashMap<>();
            resource.getMetadata().setLabels(labels);
        }
        labels.put(TEST_RUN_LABEL, testRunId);
    }

    private Optional<ListeningAddress> exposeViaPortForward(List<HasMetadata> resources) {
        Pod pod = findFirstPod(resources);
        int containerPort = findContainerPort(resources);

        portForward = client.pods().inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .portForward(containerPort);

        log.infof("Port-forwarding %s/%s:%d to localhost:%d", pod.getMetadata().getNamespace(),
                pod.getMetadata().getName(), containerPort, portForward.getLocalPort());

        return Optional.of(new ListeningAddress(portForward.getLocalPort(), "http"));
    }

    private Pod findFirstPod(List<HasMetadata> resources) {
        Optional<Deployment> deployment = resources.stream()
                .filter(Deployment.class::isInstance).map(Deployment.class::cast).findFirst();
        Map<String, String> selectorLabels = deployment
                .map(d -> d.getSpec().getSelector().getMatchLabels())
                .orElseGet(() -> Map.of(TEST_RUN_LABEL, testRunId));

        List<Pod> pods = client.pods().inNamespace(client.getNamespace()).withLabels(selectorLabels).list().getItems();
        if (pods.isEmpty()) {
            throw new IllegalStateException("No pods found for the deployed application (labels: " + selectorLabels + ")");
        }
        return pods.get(0);
    }

    private int findContainerPort(List<HasMetadata> resources) {
        return resources.stream()
                .filter(Service.class::isInstance).map(Service.class::cast)
                .findFirst()
                .map(service -> service.getSpec().getPorts().get(0).getTargetPort().getIntVal())
                .orElse(httpPort != 0 ? httpPort : 8080);
    }

    private Optional<ListeningAddress> exposeViaRouteOrIngress(List<HasMetadata> resources) {
        String host;
        String protocol = "http";
        int port = 80;

        if ("openshift".equals(deploymentTarget)) {
            Route route = resources.stream()
                    .filter(Route.class::isInstance).map(Route.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No Route found in the generated OpenShift manifest; set quarkus.openshift.route.expose=true "
                                    + "or use quarkus.test.openshift.exposure=port-forward instead"));

            if (route.getSpec().getTls() != null) {
                protocol = "https";
                port = 443;
            }

            // The router reconciles Routes asynchronously, so wait for an "Admitted" condition in its status
            // rather than assuming it's ready as soon as the Deployment is.
            log.infof("Waiting for Route %s to be admitted by the router...", route.getMetadata().getName());
            String routeName = route.getMetadata().getName();
            route = pollUntil(
                    () -> ((OpenShiftClient) client).routes().inNamespace(client.getNamespace())
                            .withName(routeName).get(),
                    r -> r != null && r.getStatus() != null && r.getStatus().getIngress() != null
                            && r.getStatus().getIngress().stream()
                                    .flatMap(routeIngress -> routeIngress.getConditions() == null
                                            ? Stream.empty()
                                            : routeIngress.getConditions().stream())
                                    .anyMatch(c -> "Admitted".equals(c.getType()) && "True".equals(c.getStatus())),
                    "Route " + routeName + " to be admitted by the router");

            // spec.host is only set if quarkus.openshift.route.host was configured explicitly; otherwise the
            // router assigns one, which is only ever reflected in the admitted status entry's host, never
            // backfilled into spec.
            host = route.getSpec().getHost();
            if (host == null || host.isBlank()) {
                String admittedRouteName = routeName;
                host = route.getStatus().getIngress().stream()
                        .filter(routeIngress -> routeIngress.getConditions() != null
                                && routeIngress.getConditions().stream()
                                        .anyMatch(c -> "Admitted".equals(c.getType()) && "True".equals(c.getStatus())))
                        .map(RouteIngress::getHost)
                        .filter(h -> h != null && !h.isBlank())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Admitted Route " + admittedRouteName + " has no host in its status"));
            }
        } else {
            Ingress ingress = resources.stream()
                    .filter(Ingress.class::isInstance).map(Ingress.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No Ingress found in the generated Kubernetes manifest; set quarkus.kubernetes.ingress.expose=true "
                                    + "or use quarkus.test.kubernetes.exposure=port-forward instead"));

            // The Deployment being ready doesn't mean the ingress controller has actually reconciled this
            // Ingress into its own routing config yet (that happens asynchronously) - wait for its status to be
            // populated, the same signal used to detect a LoadBalancer Service's external IP being assigned.
            log.infof("Waiting for Ingress %s to be admitted by the ingress controller...",
                    ingress.getMetadata().getName());
            String ingressName = ingress.getMetadata().getName();
            ingress = pollUntil(
                    () -> client.network().v1().ingresses().inNamespace(client.getNamespace()).withName(ingressName)
                            .get(),
                    i -> i != null && i.getStatus() != null && i.getStatus().getLoadBalancer() != null
                            && i.getStatus().getLoadBalancer().getIngress() != null
                            && !i.getStatus().getLoadBalancer().getIngress().isEmpty(),
                    "Ingress " + ingressName + " to be admitted by the ingress controller");

            // spec.rules[].host is only set if quarkus.kubernetes.ingress.host was configured explicitly;
            // otherwise the rule is a host-less catch-all ("*") and the only address to reach it by is whatever
            // the ingress controller's LoadBalancer status reports (a hostname, or an IP if it has no hostname).
            host = ingress.getSpec().getRules().get(0).getHost();
            if (host == null || host.isBlank()) {
                String admittedIngressName = ingressName;
                host = ingress.getStatus().getLoadBalancer().getIngress().stream()
                        .map(lbIngress -> lbIngress.getHostname() != null ? lbIngress.getHostname() : lbIngress.getIp())
                        .filter(h -> h != null && !h.isBlank())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Admitted Ingress " + admittedIngressName + " has no hostname/IP in its status"));
            }
        }

        // Overridable since the Route/Ingress host isn't necessarily reachable on the standard 80/443 (e.g. a
        // rootless container runtime that can't bind a port below 1024, so the ingress controller ends up
        // exposed on a high port instead).
        if (externalPort.isPresent()) {
            port = externalPort.getAsInt();
        }

        // This is what makes RestAssured (via TestHTTPResourceManager.testUrl()) target the Route/Ingress host
        // instead of the localhost default - ListeningAddress only carries port/protocol, not a host, so this
        // system property is the only channel left for it. Also used by waitForHttpReadiness(). close()
        // restores whatever value this overwrote.
        previousHttpHost = System.setProperty("quarkus.http.host", host);
        return Optional.of(new ListeningAddress(port, protocol));
    }

    private void waitForHttpReadiness(ListeningAddress address) {
        String scheme = address.isSsl() ? "https" : "http";
        String host = System.getProperty("quarkus.http.host", "localhost");
        URI uri = URI.create(scheme + "://" + host + ":" + address.port() + "/");

        HttpRequest request = client.getHttpClient().newHttpRequestBuilder().uri(uri).timeout(15, TimeUnit.SECONDS)
                .build();

        log.infof("Probing %s for HTTP readiness...", uri);
        // Any response - regardless of status code - proves the HTTP server is up and answering, which is all
        // this cares about; the root path isn't necessarily mapped to anything (e.g. a 404 is expected and fine).
        pollUntil(() -> {
            try {
                return client.getHttpClient().sendAsync(request, byte[].class).get(15, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }, r -> true, "HTTP readiness at " + uri);
        log.infof("%s is reachable", uri);
    }

    /**
     * Polls {@code fetch} every 1s until it returns a value matching {@code condition}, up to {@link #waitTimeout}.
     * Used instead of the fabric8 client's own {@code waitUntilCondition} (which is watch/event-driven) because it
     * only re-evaluates the predicate on a *change* event - if the condition is already true by the time the watch
     * is established, no further event ever fires, and it can hang past its own configured timeout. A
     * {@code RuntimeException} thrown by {@code fetch} is treated the same as a failed {@code condition} (retried,
     * not propagated) so callers like {@link #waitForHttpReadiness} can fold connection failures into the same
     * retry loop.
     */
    private <T> T pollUntil(Supplier<T> fetch, Predicate<T> condition, String description) {
        Instant start = Instant.now();
        Instant deadline = start.plus(waitTimeout);
        T current = null;
        RuntimeException lastFailure = null;
        while (true) {
            try {
                current = fetch.get();
                if (condition.test(current)) {
                    return current;
                }
                log.infof("Still waiting for %s (elapsed %ds)", description,
                        Duration.between(start, Instant.now()).toSeconds());
            } catch (RuntimeException e) {
                lastFailure = e;
                log.infof("Attempt for %s failed (%s), retrying...", description, e);
            }
            if (!Instant.now().isBefore(deadline)) {
                throw new IllegalStateException("Timed out after " + waitTimeout + " waiting for " + description,
                        lastFailure);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + description, e);
            }
        }
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        throw new UnsupportedOperationException(
                "Running a Quarkus application to completion (e.g. Quarkus CLI applications) is not supported "
                        + "when launching against a Kubernetes/OpenShift cluster");
    }

    @Override
    public void includeAsSysProps(Map<String, String> systemProps) {
        // Not applicable: the deployed manifest already carries its own environment/config baked in
        // at build time; there's no running local process to pass these to.
    }

    @Override
    public void close() {
        if (portForward != null) {
            try {
                portForward.close();
            } catch (IOException e) {
                log.warn("Unable to close port-forward", e);
            }
        }

        if (previousHttpHost != null) {
            System.setProperty("quarkus.http.host", previousHttpHost);
        } else {
            System.clearProperty("quarkus.http.host");
        }

        if (client != null) {
            if (deleteAfterTest) {
                Collections.reverse(appliedResources);
                for (HasMetadata resource : appliedResources) {
                    try {
                        client.resource(resource).delete();
                    } catch (RuntimeException e) {
                        log.warnf(e, "Unable to delete %s %s", resource.getKind(), resource.getMetadata().getName());
                    }
                }
            } else {
                log.infof("quarkus.test.%s.delete-after-test=false, leaving deployed resources (run id: %s)",
                        deploymentTarget, testRunId);
            }
            client.close();
        }
    }

    private static void runProcess(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                        "Command '" + String.join(" ", command) + "' failed with exit code " + exitCode + ": " + output);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while running: " + String.join(" ", command), e);
        }
    }
}
