package io.quarkus.kubernetes.client.deployment;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;
import static io.quarkus.kubernetes.client.runtime.internal.KubernetesDevServicesBuildTimeConfig.Flavor.api_only;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.ApiServerContainerVersion;
import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.K3sContainerVersion;
import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.KubernetesImageSpec;
import com.dajudge.kindcontainer.KubernetesVersionEnum;
import com.dajudge.kindcontainer.client.KubeConfigUtils;
import com.dajudge.kindcontainer.client.config.Cluster;
import com.dajudge.kindcontainer.client.config.ClusterSpec;
import com.dajudge.kindcontainer.client.config.Context;
import com.dajudge.kindcontainer.client.config.ContextSpec;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.User;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.*;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesDevServicesBuildTimeConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesDevServicesBuildTimeConfig.Flavor;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceInfoBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesDevServiceRequestBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { DevServicesConfig.Enabled.class, NoQuarkusTestKubernetesClient.class })
public class DevServicesKubernetesProcessor {
    private static final String KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG = "quarkus.kubernetes-client.devservices.override-kubeconfig";
    private static final Logger log = Logger.getLogger(DevServicesKubernetesProcessor.class);
    private static final String KUBERNETES_CLIENT_MASTER_URL = "quarkus.kubernetes-client.api-server-url";
    private static final String DEFAULT_MASTER_URL_ENDING_WITH_SLASH = Config.DEFAULT_MASTER_URL + "/";

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-kubernetes";
    static final int KUBERNETES_PORT = 6443;
    private static final ContainerLocator KubernetesContainerLocator = locateContainerWithLabels(KUBERNETES_PORT,
            DEV_SERVICE_LABEL);
    static volatile RunningDevService devService;
    static volatile KubernetesDevServiceCfg cfg;
    static volatile boolean first = true;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public DevServicesResultBuildItem setupKubernetesDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            KubernetesClientBuildConfig kubernetesClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig,
            BuildProducer<KubernetesDevServiceInfoBuildItem> devServicesKube,
            Optional<KubernetesDevServiceRequestBuildItem> devServiceKubeRequest) {

        KubernetesDevServiceCfg configuration = getConfiguration(kubernetesClientBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheCluster = !configuration.equals(cfg);
            if (!shouldShutdownTheCluster) {
                return devService.toBuildItem();
            }
            shutdownCluster();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Kubernetes Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            devService = startKubernetes(dockerStatusBuildItem, composeProjectBuildItem, configuration, launchMode,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout(),
                    devServicesKube,
                    devServiceKubeRequest);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownCluster();
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        if (devService.isOwner()) {
            log.info(
                    "Dev Services for Kubernetes started. Other Quarkus applications in dev mode will find the "
                            + "cluster automatically.");
        }

        return devService.toBuildItem();
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
                // Load the manifest from the resources directory
                InputStream manifestStream = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(manifestPath);

                if (manifestStream == null) {
                    log.errorf("Could not find manifest file in resources: %s", manifestPath);
                    continue;
                }

                try (manifestStream) {
                    try {
                        // A single manifest file may contain multiple resources to deploy
                        List<HasMetadata> resources = client.load(manifestStream).items();

                        if (resources.isEmpty()) {
                            log.warnf("No resources found in manifest: %s", manifestPath);
                        } else {
                            resources.forEach(resource -> client.resource(resource).create());
                        }
                    } catch (Exception ex) {
                        log.errorf("Failed to deploy manifest %s: %s", manifestPath, ex.getMessage());
                    }
                }

                log.infof("Applied manifest %s.", manifestPath);
            }
        } catch (Exception e) {
            log.error("Failed to create Kubernetes client while trying to deploy manifests.", e);
        }
    }

    private void shutdownCluster() {
        if (devService != null && devService.isOwner()) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Kubernetes cluster", e);
            } finally {
                devService = null;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "OptionalUsedAsFieldOrParameterType" })
    private RunningDevService startKubernetes(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            KubernetesDevServiceCfg config,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout,
            BuildProducer<KubernetesDevServiceInfoBuildItem> devServicesKube,
            Optional<KubernetesDevServiceRequestBuildItem> devServiceKubeRequest) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Kubernetes, as it has been disabled in the config.");
            return null;
        }

        // Check if kubernetes-client.api-server-url is set
        if (ConfigUtils.isPropertyNonEmpty(KUBERNETES_CLIENT_MASTER_URL)) {
            log.debug("Not starting Dev Services for Kubernetes as the client has been explicitly configured via "
                    + KUBERNETES_CLIENT_MASTER_URL);
            return null;
        }

        // If we have an explicit request coming from extensions, start even if there's a non-explicitly overridden kube config
        final boolean shouldStart = config.overrideKubeconfig || devServiceKubeRequest.isPresent();
        if (!shouldStart) {
            var autoConfigMasterUrl = Config.autoConfigure(null).getMasterUrl();
            if (!DEFAULT_MASTER_URL_ENDING_WITH_SLASH.equals(autoConfigMasterUrl)) {
                log.debug(
                        "Not starting Dev Services for Kubernetes as a kube config file has been found. Set "
                                + KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG
                                + " to true to disregard the config and start Dev Services for Kubernetes.");
                return null;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "A running container runtime is required for Dev Services to work. Please check if your container runtime is running.");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = KubernetesContainerLocator.locateContainer(config.serviceName,
                config.shared,
                launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of("kube-apiserver", "k3s", "kindest/node"),
                        KUBERNETES_PORT, launchMode.getLaunchMode(), useSharedNetwork));

        final Supplier<RunningDevService> defaultKubernetesClusterSupplier = () -> {
            Flavor clusterType = config.flavor
                    .or(() -> devServiceKubeRequest
                            .map(KubernetesDevServiceRequestBuildItem::getFlavor)
                            .map(Flavor::valueOf))
                    .orElse(api_only);

            @SuppressWarnings("rawtypes")
            KubernetesContainer container = switch (clusterType) {
                case api_only -> createContainer(ApiServerContainer::new, ApiServerContainerVersion.class, config, clusterType);

                case k3s -> createContainer(K3sContainer::new, K3sContainerVersion.class, config, clusterType);

                case kind -> createContainer(KindContainer::new, KindContainerVersion.class, config, clusterType);
            };

            if (useSharedNetwork) {
                ConfigureUtil.configureSharedNetwork(container, "quarkus-kubernetes-client");
            }
            if (config.serviceName != null) {
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName);
                container.withLabel(QUARKUS_DEV_SERVICE, config.serviceName);
            }
            timeout.ifPresent(container::withStartupTimeout);

            container.withEnv(config.containerEnv);

            container.start();

            KubeConfig kubeConfig = KubeConfigUtils.parseKubeConfig(container.getKubeconfig());

            devServicesKube
                    .produce(new KubernetesDevServiceInfoBuildItem(container.getKubeconfig(), container.getContainerId()));

            return new RunningDevService(Feature.KUBERNETES_CLIENT.getName(), container.getContainerId(),
                    new ContainerShutdownCloseable(container, Feature.KUBERNETES_CLIENT.getName()),
                    getKubernetesClientConfigFromKubeConfig(kubeConfig));
        };

        return maybeContainerAddress
                .map(containerAddress -> new RunningDevService(Feature.KUBERNETES_CLIENT.getName(),
                        containerAddress.getId(),
                        null,
                        resolveConfigurationFromRunningContainer(containerAddress)))
                .orElseGet(defaultKubernetesClusterSupplier);
    }

    @SuppressWarnings("rawtypes")
    private <T extends KubernetesVersionEnum<T>, C extends KubernetesContainer> C createContainer(
            Function<KubernetesImageSpec<T>, C> constructor,
            Class<T> versionClass,
            KubernetesDevServiceCfg config,
            Flavor flavor) {
        T version = config.apiVersion
                .map(v -> findOrElseThrow(flavor, v, versionClass))
                .orElseGet(() -> latest(versionClass));

        KubernetesImageSpec<T> imageSpec = version.withImage(config.imageName);
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

    private Map<String, String> getKubernetesClientConfigFromKubeConfig(KubeConfig kubeConfig) {

        ClusterSpec cluster = kubeConfig.getClusters().get(0).getCluster();
        UserSpec user = kubeConfig.getUsers().get(0).getUser();
        return Map.of(
                KUBERNETES_CLIENT_MASTER_URL, cluster.getServer(),
                "quarkus.kubernetes-client.ca-cert-data",
                cluster.getCertificateAuthorityData(),
                "quarkus.kubernetes-client.client-cert-data",
                user.getClientCertificateData(),
                "quarkus.kubernetes-client.client-key-data", user.getClientKeyData(),
                "quarkus.kubernetes-client.client-key-algo", Config.getKeyAlgorithm(null, user.getClientKeyData()),
                "quarkus.kubernetes-client.namespace", "default");
    }

    private Map<String, String> resolveConfigurationFromRunningContainer(ContainerAddress containerAddress) {
        var dockerClient = DockerClientFactory.lazyClient();
        var container = new RunningContainer(dockerClient, containerAddress);

        return container.getKubeconfig();
    }

    private KubernetesDevServiceCfg getConfiguration(KubernetesClientBuildConfig cfg) {
        KubernetesDevServicesBuildTimeConfig devServicesConfig = cfg.devservices();
        return new KubernetesDevServiceCfg(devServicesConfig);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class KubernetesDevServiceCfg {

        public boolean devServicesEnabled;
        public String imageName;
        public Optional<Flavor> flavor;
        public Optional<String> apiVersion;
        public boolean overrideKubeconfig;
        public boolean shared;
        public String serviceName;
        public Map<String, String> containerEnv;
        public Optional<List<String>> manifests;

        public KubernetesDevServiceCfg(KubernetesDevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled();
            this.imageName = config.imageName()
                    .orElse(null);
            this.serviceName = config.serviceName();
            this.apiVersion = config.apiVersion();
            this.overrideKubeconfig = config.overrideKubeconfig();
            this.flavor = config.flavor();
            this.shared = config.shared();
            this.containerEnv = config.containerEnv();
            this.manifests = config.manifests();
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, flavor, apiVersion, overrideKubeconfig, shared, serviceName,
                    containerEnv);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof KubernetesDevServiceCfg other))
                return false;
            return devServicesEnabled == other.devServicesEnabled && flavor == other.flavor
                    && Objects.equals(apiVersion, other.apiVersion) && overrideKubeconfig == other.overrideKubeconfig
                    && shared == other.shared && Objects.equals(serviceName, other.serviceName)
                    && Objects.equals(containerEnv, other.containerEnv);
        }
    }

    private class RunningContainer implements ContainerState {
        private static final String KIND_KUBECONFIG = "/etc/kubernetes/admin.conf";
        private static final String K3S_KUBECONFIG = "/etc/rancher/k3s/k3s.yaml";
        private static final String APISERVER = "apiserver";
        private static final String PKI_BASEDIR = "/etc/kubernetes/pki";
        private static final String API_SERVER_CA = PKI_BASEDIR + "/ca.crt";
        private static final String API_SERVER_CERT = PKI_BASEDIR + "/apiserver.crt";
        private static final String API_SERVER_KEY = PKI_BASEDIR + "/apiserver.key";

        private final DockerClient dockerClient;

        private final InspectContainerResponse containerInfo;

        private final ContainerAddress containerAddress;

        public RunningContainer(DockerClient dockerClient, ContainerAddress containerAddress) {
            this.dockerClient = dockerClient;
            this.containerAddress = containerAddress;
            this.containerInfo = dockerClient.inspectContainerCmd(getContainerId()).exec();
        }

        public Map<String, String> getKubeconfig() {
            var image = getContainerInfo().getConfig().getImage();
            if (image.contains("rancher/k3s")) {
                return getKubernetesClientConfigFromKubeConfig(
                        KubeConfigUtils.parseKubeConfig(KubeConfigUtils.replaceServerInKubeconfig(containerAddress.getUrl(),
                                getFileContentFromContainer(K3S_KUBECONFIG))));
            } else if (image.contains("kindest/node")) {
                return getKubernetesClientConfigFromKubeConfig(
                        KubeConfigUtils.parseKubeConfig(KubeConfigUtils.replaceServerInKubeconfig(containerAddress.getUrl(),
                                getFileContentFromContainer(KIND_KUBECONFIG))));
            } else if (image.contains("k8s.gcr.io/kube-apiserver") ||
                    image.contains("registry.k8s.io/kube-apiserver")) {
                return getKubernetesClientConfigFromKubeConfig(getKubeconfigFromApiContainer(containerAddress.getUrl()));
            }

            // this can happen only if the user manually start
            // a DEV_SERVICE_LABEL labeled container with an invalid image name
            throw new RuntimeException("The container with the label '" + DEV_SERVICE_LABEL
                    + "' is not compatible with Dev Services for Kubernetes. Stop it or disable Dev Services for Kubernetes.");
        }

        protected KubeConfig getKubeconfigFromApiContainer(final String url) {
            final Cluster cluster = new Cluster();
            cluster.setName(APISERVER);
            cluster.setCluster(new ClusterSpec());
            cluster.getCluster().setServer(url);
            cluster.getCluster().setCertificateAuthorityData((base64(getFileContentFromContainer(API_SERVER_CA))));
            final User user = new User();
            user.setName(APISERVER);
            user.setUser(new UserSpec());
            user.getUser().setClientKeyData(base64(getFileContentFromContainer(API_SERVER_KEY)));
            user.getUser().setClientCertificateData(base64(getFileContentFromContainer(API_SERVER_CERT)));
            final Context context = new Context();
            context.setName(APISERVER);
            context.setContext(new ContextSpec());
            context.getContext().setCluster(cluster.getName());
            context.getContext().setUser(user.getName());
            final KubeConfig config = new KubeConfig();
            config.setUsers(singletonList(user));
            config.setClusters(singletonList(cluster));
            config.setContexts(singletonList(context));
            config.setCurrentContext(context.getName());
            return config;
        }

        private String base64(final String str) {
            return Base64.getEncoder().encodeToString(str.getBytes(US_ASCII));
        }

        @Override
        public List<Integer> getExposedPorts() {
            return List.of(containerAddress.getPort());
        }

        @Override
        public DockerClient getDockerClient() {
            return this.dockerClient;
        }

        @Override
        public InspectContainerResponse getContainerInfo() {
            return containerInfo;
        }

        @Override
        public String getContainerId() {
            return this.containerAddress.getId();
        }

        public String getFileContentFromContainer(String containerPath) {
            return copyFileFromContainer(containerPath, this::readString);
        }

        String readString(final InputStream is) throws IOException {
            return new String(readBytes(is), UTF_8);
        }

        private byte[] readBytes(final InputStream is) throws IOException {
            final byte[] buffer = new byte[1024];
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) > 0) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        }
    }
}
