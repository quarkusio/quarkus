package io.quarkus.apicurio.registry.devservice;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.apicurio.registry.devservice.ApicurioRegistryBuildTimeConfig.ApicurioRegistryDevServicesBuildTimeConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts Apicurio Registry as dev service if needed.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class DevServicesApicurioRegistryProcessor {

    private static final Logger log = Logger.getLogger(DevServicesApicurioRegistryProcessor.class);

    private static final int APICURIO_REGISTRY_PORT = 8080; // inside the container
    private static final String APICURIO_REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url";
    private static final String CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.schema.registry.url";

    /**
     * Label to add to shared Dev Service for Apicurio Registry running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-apicurio-registry";

    private static final ContainerLocator apicurioRegistryContainerLocator = locateContainerWithLabels(APICURIO_REGISTRY_PORT,
            DEV_SERVICE_LABEL);

    static volatile RunningDevService devService;
    static volatile ApicurioRegistryDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startApicurioRegistryDevService(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ApicurioRegistryBuildTimeConfig apicurioRegistryConfiguration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, DevServicesConfig devServicesConfig) {

        ApicurioRegistryDevServiceCfg configuration = getConfiguration(apicurioRegistryConfiguration.devservices());

        if (devService != null) {
            boolean restartRequired = !configuration.equals(cfg);
            if (!restartRequired) {
                return devService.toBuildItem();
            }
            shutdownApicurioRegistry();
            cfg = null;
        }
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Apicurio Registry Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startApicurioRegistry(dockerStatusBuildItem, composeProjectBuildItem, configuration, launchMode,
                    useSharedNetwork, devServicesConfig.timeout());
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        cfg = configuration;

        if (devService.isOwner()) {
            log.infof("Dev Services for Apicurio Registry started. The registry is available at %s",
                    devService.getConfig().get(APICURIO_REGISTRY_URL_CONFIG));
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (devService != null) {
                        shutdownApicurioRegistry();
                    }
                    first = true;
                    devService = null;
                    cfg = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        return devService.toBuildItem();
    }

    private Map<String, String> getRegistryUrlConfigs(String baseUrl) {
        return Map.of(
                APICURIO_REGISTRY_URL_CONFIG, baseUrl + "/apis/registry/v2",
                CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, baseUrl + "/apis/ccompat/v6");
    }

    private void shutdownApicurioRegistry() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop Apicurio Registry", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startApicurioRegistry(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ApicurioRegistryDevServiceCfg config, LaunchModeBuildItem launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for Apicurio Registry, as it has been disabled in the config.");
            return null;
        }

        if (ConfigUtils.isPropertyNonEmpty(APICURIO_REGISTRY_URL_CONFIG)) {
            log.debug("Not starting dev services for Apicurio Registry, " + APICURIO_REGISTRY_URL_CONFIG + " is configured.");
            return null;
        }

        if (ConfigUtils.isPropertyNonEmpty(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG)) {
            log.debug("Not starting dev services for Apicurio Registry, " + CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG
                    + " is configured.");
            return null;
        }

        if (!hasKafkaChannelWithoutRegistry()) {
            log.debug("Not starting dev services for Apicurio Registry, all the channels have a registry URL configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please run Apicurio Registry yourself.");
            return null;
        }

        // Starting the broker
        return apicurioRegistryContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(config.imageName, "apicurio"),
                        APICURIO_REGISTRY_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(address -> new RunningDevService(Feature.APICURIO_REGISTRY_AVRO.getName(),
                        address.getId(), null,
                        // address does not have the URL Scheme - just the host:port, so prepend http://
                        getRegistryUrlConfigs("http://" + address.getUrl())))
                .orElseGet(() -> {
                    ApicurioRegistryContainer container = new ApicurioRegistryContainer(
                            DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("apicurio/apicurio-registry-mem"),
                            config.fixedExposedPort,
                            launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                            composeProjectBuildItem.getDefaultNetworkId(),
                            useSharedNetwork);
                    timeout.ifPresent(container::withStartupTimeout);
                    container.withEnv(config.containerEnv);
                    container.start();

                    return new RunningDevService(Feature.APICURIO_REGISTRY_AVRO.getName(), container.getContainerId(),
                            container::close, getRegistryUrlConfigs(container.getUrl()));
                });
    }

    private boolean hasKafkaChannelWithoutRegistry() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isKafka = isConnector
                    && "smallrye-kafka".equals(config.getOptionalValue(name, String.class).orElse("ignored"));
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isKafka) {
                isConfigured = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".apicurio.registry.url"))
                        || ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".schema.registry.url"));
            }
            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private ApicurioRegistryDevServiceCfg getConfiguration(ApicurioRegistryDevServicesBuildTimeConfig cfg) {
        return new ApicurioRegistryDevServiceCfg(cfg);
    }

    private static final class ApicurioRegistryDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, String> containerEnv;

        public ApicurioRegistryDevServiceCfg(ApicurioRegistryDevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled().orElse(true);
            this.imageName = config.imageName();
            this.fixedExposedPort = config.port().orElse(0);
            this.shared = config.shared();
            this.serviceName = config.serviceName();
            this.containerEnv = config.containerEnv();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ApicurioRegistryDevServiceCfg that = (ApicurioRegistryDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && shared == that.shared
                    && Objects.equals(serviceName, that.serviceName)
                    && Objects.equals(containerEnv, that.containerEnv);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort, shared, serviceName, containerEnv);
        }
    }

    private static final class ApicurioRegistryContainer extends GenericContainer<ApicurioRegistryContainer> {
        private final int fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        private ApicurioRegistryContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            withEnv("QUARKUS_PROFILE", "prod");
            if (!dockerImageName.getRepository().endsWith("apicurio/apicurio-registry-mem")) {
                throw new IllegalArgumentException("Only apicurio/apicurio-registry-mem images are supported");
            }
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "apicurio-registry");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort > 0) {
                addFixedExposedPort(fixedExposedPort, APICURIO_REGISTRY_PORT);
            } else {
                addExposedPorts(APICURIO_REGISTRY_PORT);
            }
        }

        public String getUrl() {
            return String.format("http://%s:%s", getHostToUse(), getPortToUse());
        }

        private String getHostToUse() {
            return useSharedNetwork ? hostName : getHost();
        }

        private int getPortToUse() {
            return useSharedNetwork ? APICURIO_REGISTRY_PORT : getMappedPort(APICURIO_REGISTRY_PORT);
        }
    }
}
