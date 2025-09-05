package io.quarkus.kubernetes.client.deployment;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.ApiServerContainerVersion;
import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.K3sContainerVersion;
import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.KubernetesImageSpec;
import com.dajudge.kindcontainer.KubernetesVersionEnum;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesDevServicesBuildTimeConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesDevServicesBuildTimeConfig.Flavor;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceInfoBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceRequestBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class,
        NoQuarkusTestKubernetesClient.class })
public class DevServicesKubernetesProcessor {
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-kubernetes";
    static final int KUBERNETES_PORT = 6443;
    private static final String KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG = "quarkus.kubernetes-client.devservices.override-kubeconfig";
    private static final Logger log = Logger.getLogger(DevServicesKubernetesProcessor.class);
    static final String KUBERNETES_CLIENT_MASTER_URL = "quarkus.kubernetes-client.api-server-url";
    private static final String DEFAULT_MASTER_URL_ENDING_WITH_SLASH = Config.DEFAULT_MASTER_URL + "/";
    private static final ContainerLocator KubernetesContainerLocator = locateContainerWithLabels(KUBERNETES_PORT,
            DEV_SERVICE_LABEL);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public DevServicesResultBuildItem setupKubernetesDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchModeBI,
            KubernetesClientBuildConfig kubernetesClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetwork,
            DevServicesConfig devServicesConfig,
            BuildProducer<KubernetesDevServiceInfoBuildItem> devServicesKube,
            Optional<KubernetesDevServiceRequestBuildItem> devServiceKubeRequest) {
        // If the dev service is disabled, we return null to indicate that no dev service was started.
        final var kubeDevServiceConfig = kubernetesClientBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, kubeDevServiceConfig, devServiceKubeRequest)) {
            return null;
        }

        final var launchMode = launchModeBI.getLaunchMode();
        final var useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetwork);
        final var serviceName = kubeDevServiceConfig.serviceName();
        return KubernetesContainerLocator
                .locateContainer(serviceName, kubeDevServiceConfig.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of("kube-apiserver", "k3s", "kindest/node"),
                        KUBERNETES_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    final var container = RunningKubeContainer.create(containerAddress);

                    devServicesKube.produce(new KubernetesDevServiceInfoBuildItem(
                            new KubernetesDevServiceInfoBuildItem.ConfigurationSupplier() {
                                @Override
                                public String getKubeConfig() {
                                    return container.getKubeConfigAsString();
                                }

                                @Override
                                public String getContainerId() {
                                    return containerAddress.getId();
                                }
                            }));

                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.KUBERNETES_CLIENT)
                            .containerId(containerAddress.getId())
                            .config(container.getKubernetesClientConfig())
                            .build();
                })
                .orElseGet(() -> {
                    final var container = createContainer(kubeDevServiceConfig,
                            devServiceKubeRequest,
                            useSharedNetwork,
                            devServicesConfig.timeout());
                    final var kubeContainer = container.getContainer();
                    final var result = DevServicesResultBuildItem.owned()
                            .feature(Feature.KUBERNETES_CLIENT)
                            .serviceName(serviceName)
                            .serviceConfig(kubeDevServiceConfig)
                            .startable(() -> container)
                            .configProvider(
                                    StartableKubernetesContainer.getKubernetesClientConfigFromRunningContainerKubeConfig())
                            .postStartHook(unused -> log.info(
                                    "Dev Services for Kubernetes started. Other Quarkus applications in dev mode will find the cluster automatically."))
                            .build();

                    devServicesKube.produce(new KubernetesDevServiceInfoBuildItem(
                            new KubernetesDevServiceInfoBuildItem.ConfigurationSupplier() {
                                @Override
                                public String getKubeConfig() {
                                    return kubeContainer.getKubeconfig();
                                }

                                @Override
                                public String getContainerId() {
                                    return kubeContainer.getContainerId();
                                }
                            }));

                    return result;
                });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem,
            KubernetesDevServicesBuildTimeConfig config,
            Optional<KubernetesDevServiceRequestBuildItem> devServiceKubeRequest) {
        if (!config.enabled()) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Kubernetes, as it has been disabled in the config.");
            return true;
        }

        // Check if kubernetes-client.api-server-url is set
        if (ConfigUtils.isPropertyNonEmpty(KUBERNETES_CLIENT_MASTER_URL)) {
            log.debug("Not starting Dev Services for Kubernetes as the client has been explicitly configured via "
                    + KUBERNETES_CLIENT_MASTER_URL);
            return true;
        }

        // If we have an explicit request coming from extensions, start even if there's a non-explicitly overridden kube config
        final boolean shouldStart = config.overrideKubeconfig() || devServiceKubeRequest.isPresent();
        if (!shouldStart) {
            var autoConfigMasterUrl = Config.autoConfigure(null).getMasterUrl();
            if (!DEFAULT_MASTER_URL_ENDING_WITH_SLASH.equals(autoConfigMasterUrl)) {
                log.debug(
                        "Not starting Dev Services for Kubernetes as a kube config file has been found. Set "
                                + KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG
                                + " to true to disregard the config and start Dev Services for Kubernetes.");
                return true;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "A running container runtime is required for Dev Services to work. Please check if your container runtime is running.");
            return true;
        }

        return false;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StartableKubernetesContainer createContainer(KubernetesDevServicesBuildTimeConfig config,
            Optional<KubernetesDevServiceRequestBuildItem> devServiceKubeRequest,
            boolean useSharedNetwork,
            Optional<Duration> timeout) {
        Flavor clusterType = devServiceKubeRequest
                .map(kdsr -> Flavor.valueOf(kdsr.getFlavor()))
                .orElse(config.flavor());

        KubernetesContainer<?> container = switch (clusterType) {
            case api_only -> createContainer(ApiServerContainer::new, ApiServerContainerVersion.class, config, clusterType);

            case k3s -> createContainer(K3sContainer::new, K3sContainerVersion.class, config, clusterType);

            case kind -> createContainer(KindContainer::new, KindContainerVersion.class, config, clusterType);
        };

        if (useSharedNetwork) {
            ConfigureUtil.configureSharedNetwork(container, "quarkus-kubernetes-client");
        }
        final String serviceName = config.serviceName();
        if (serviceName != null) {
            container.withLabel(DEV_SERVICE_LABEL, serviceName);
            container.withLabel(QUARKUS_DEV_SERVICE, serviceName);
        }
        timeout.ifPresent(container::withStartupTimeout);

        container.withEnv(config.containerEnv());
        return new StartableKubernetesContainer(container);
    }

    /**
     * Deploys a set of manifests as files in the resources directory to the Kubernetes dev service.
     * This build step produces a {@link ServiceStartBuildItem} that ensures the Build Step always runs even if no other build
     * step consumes it.
     *
     * @param kubernetesDevServiceInfoBuildItem This ensures the manifests are deployed after the Kubernetes dev service is
     *        started.
     * @param kubernetesClientBuildTimeConfig This config is used to read the extension configuration for dev services.
     */
    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    public void applyManifests(
            KubernetesDevServiceInfoBuildItem kubernetesDevServiceInfoBuildItem,
            KubernetesClientBuildConfig kubernetesClientBuildTimeConfig) {
        if (kubernetesDevServiceInfoBuildItem == null) {
            // Gracefully return in case the Kubernetes dev service could not be spun up.
            log.warn("Cannot apply manifests because the Kubernetes dev service is not running");
            return;
        }

        var manifests = kubernetesClientBuildTimeConfig.devservices().manifests();

        // Do not run the manifest deployment if no manifests are configured
        if (manifests.isEmpty())
            return;

        try (KubernetesClient client = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubernetesDevServiceInfoBuildItem.getKubeConfig()))
                .build()) {
            for (String manifestPath : manifests.get()) {
                InputStream manifestStream = getManifestStream(manifestPath);

                if (manifestStream == null) {
                    log.errorf("Could not find manifest file in resources: %s", manifestPath);
                    continue;
                }

                try (manifestStream) {
                    try {
                        // A single manifest file may contain multiple resources to deploy
                        List<HasMetadata> resources = client.load(manifestStream).items();
                        List<HasMetadata> resourcesWithReadiness = new ArrayList<>();

                        if (resources.isEmpty()) {
                            log.warnf("No resources found in manifest: %s", manifestPath);
                        } else {
                            resources.forEach(resource -> {
                                client.resource(resource).create();

                                if (isReadinessApplicable(resource)) {
                                    resourcesWithReadiness.add(resource);
                                }
                            });

                            resourcesWithReadiness.forEach(resource -> {
                                log.info("Waiting for " + resource.getClass().getSimpleName() + " "
                                        + resource.getMetadata().getName()
                                        + " to be ready...");
                                client.resource(resource).waitUntilReady(60, TimeUnit.SECONDS);
                            });

                            log.infof("Applied manifest %s.", manifestPath);
                        }
                    } catch (Exception ex) {
                        log.errorf("Failed to apply manifest %s: %s", manifestPath, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to create Kubernetes client while trying to apply manifests.", e);
        }
    }

    private boolean isReadinessApplicable(HasMetadata item) {
        return (item instanceof Deployment ||
                item instanceof io.fabric8.kubernetes.api.model.extensions.Deployment ||
                item instanceof ReplicaSet ||
                item instanceof Pod ||
                item instanceof ReplicationController ||
                item instanceof Endpoints ||
                item instanceof Node ||
                item instanceof StatefulSet);
    }

    private InputStream getManifestStream(String manifestPath) throws IOException {
        try {
            URL url = new URL(manifestPath);
            // For file:// URLs, optionally check if you want to support those or not
            return url.openStream();
        } catch (MalformedURLException e) {
            // Not a URL, so treat as classpath resource
            InputStream stream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(manifestPath);
            if (stream == null) {
                throw new IOException("Resource not found: " + manifestPath);
            }
            return stream;
        }
    }

    @SuppressWarnings("rawtypes")
    private <T extends KubernetesVersionEnum<T>, C extends KubernetesContainer> C createContainer(
            Function<KubernetesImageSpec<T>, C> constructor,
            Class<T> versionClass,
            KubernetesDevServicesBuildTimeConfig config,
            Flavor flavor) {
        T version = config.apiVersion()
                .map(v -> findOrElseThrow(flavor, v, versionClass))
                .orElseGet(() -> latest(versionClass));

        KubernetesImageSpec<T> imageSpec = version.withImage(config.imageName().orElse(null));
        return constructor.apply(imageSpec);
    }

    private <T extends KubernetesVersionEnum<T>> T findOrElseThrow(final Flavor flavor, final String version,
            final Class<T> versions) {
        final String versionWithPrefix = !version.startsWith("v") ? "v" + version : version;
        return KubernetesVersionEnum.ascending(versions)
                .stream()
                .filter(v -> v.descriptor().getKubernetesVersion().startsWith(versionWithPrefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Invalid API version '%s' for flavor '%s'. Options are: [%s]", versionWithPrefix, flavor,
                                KubernetesVersionEnum.ascending(versions).stream()
                                        .map(v -> v.descriptor().getKubernetesVersion())
                                        .collect(Collectors.joining(", ")))));
    }

}
