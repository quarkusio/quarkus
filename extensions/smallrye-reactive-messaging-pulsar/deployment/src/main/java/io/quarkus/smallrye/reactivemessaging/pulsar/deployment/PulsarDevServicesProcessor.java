package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

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
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a Pulsar broker as dev service if needed.
 * It uses https://hub.docker.com/r/apachepulsar/pulsar as image.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class PulsarDevServicesProcessor {

    private static final Logger log = Logger.getLogger(PulsarDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for pulsar running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-pulsar";

    private static final ContainerLocator pulsarContainerLocator = locateContainerWithLabels(PulsarContainer.BROKER_PORT,
            DEV_SERVICE_LABEL);

    private static final String PULSAR_CLIENT_SERVICE_URL = "pulsar.client.serviceUrl";
    private static final String PULSAR_ADMIN_SERVICE_URL = "pulsar.admin.serviceUrl";
    static final String DEV_SERVICE_PULSAR = "pulsar";
    static volatile RunningDevService devService;
    static volatile PulsarDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startPulsarDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            PulsarBuildTimeConfig pulsarClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        PulsarDevServiceCfg configuration = getConfiguration(pulsarClientBuildTimeConfig);
        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Pulsar Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startPulsarContainer(dockerStatusBuildItem, composeProjectBuildItem,
                    configuration, launchMode,
                    useSharedNetwork, devServicesConfig.timeout());
            if (newDevService != null) {
                devService = newDevService;
                Map<String, String> config = devService.getConfig();
                if (newDevService.isOwner()) {
                    log.info("Dev Services for Pulsar started.");
                    log.infof("Other Quarkus applications in dev mode will find the "
                            + "broker automatically. For Quarkus applications in production mode, you can connect to"
                            + " this by starting your application with -Dpulsar.client.serviceUrl=%s",
                            config.get(PULSAR_CLIENT_SERVICE_URL));
                }
            }
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
                    shutdownBroker();

                    log.info("Dev Services for Pulsar shut down.");
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;
        return devService.toBuildItem();
    }

    private void shutdownBroker() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Pulsar broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startPulsarContainer(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            PulsarDevServiceCfg config,
            LaunchModeBuildItem launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for Pulsar, as it has been disabled in the config.");
            return null;
        }

        // Check if pulsar.serviceUrl is set
        if (ConfigUtils.isPropertyNonEmpty(PULSAR_CLIENT_SERVICE_URL)) {
            log.debug("Not starting Dev Services for Pulsar, the pulsar.serviceUrl is configured.");
            return null;
        }

        // Verify that we have Pulsar channels without host and port
        if (!hasPulsarChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for Pulsar, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the Pulsar broker location.");
            return null;
        }

        final Supplier<RunningDevService> defaultPulsarBrokerSupplier = () -> {
            // Starting the broker
            PulsarContainer container = new PulsarContainer(DockerImageName.parse(config.imageName)
                    .asCompatibleSubstituteFor("apachepulsar/pulsar"),
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork);
            config.brokerConfig.forEach((key, value) -> container.addEnv("PULSAR_PREFIX_" + key, value));
            if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) { // Only adds the label in dev mode.
                container.withLabel(DEV_SERVICE_LABEL, config.serviceName);
                container.withLabel(QUARKUS_DEV_SERVICE, config.serviceName);
            }
            if (config.fixedExposedPort != 0) {
                container.withPort(config.fixedExposedPort);
            }
            timeout.ifPresent(container::withStartupTimeout);
            container.start();

            return getRunningService(container.getContainerId(), container::close, container.getPulsarBrokerUrl(),
                    container.getHttpServiceUrl());
        };

        return pulsarContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> getRunningService(containerAddress.getId(), null,
                        getServiceUrl(containerAddress.getHost(), containerAddress.getPort()),
                        getHttpServiceUrl(containerAddress.getHost(),
                                pulsarContainerLocator.locatePublicPort(config.serviceName, config.shared,
                                        launchMode.getLaunchMode(), PulsarContainer.BROKER_HTTP_PORT).orElse(8080))))
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(config.imageName, "pulsar"),
                        PulsarContainer.BROKER_PORT, launchMode.getLaunchMode(), useSharedNetwork)
                        .map(this::getRunningService))
                .orElseGet(defaultPulsarBrokerSupplier);
    }

    private String getServiceUrl(String host, int port) {
        return String.format("pulsar://%s:%d", host, port);
    }

    private String getHttpServiceUrl(String host, int port) {
        return String.format("http://%s:%d", host, port);
    }

    private RunningDevService getRunningService(ContainerAddress address) {
        RunningContainer container = address.getRunningContainer();
        if (container == null) {
            return null;
        }
        int httpPort = container.getPortMapping(PulsarContainer.BROKER_HTTP_PORT).orElse(8080);
        return getRunningService(address.getId(), null,
                getServiceUrl(address.getHost(), address.getPort()),
                getHttpServiceUrl(address.getHost(), httpPort));
    }

    private RunningDevService getRunningService(String containerId, Closeable closeable, String pulsarBrokerUrl,
            String httpServiceUrl) {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(PULSAR_CLIENT_SERVICE_URL, pulsarBrokerUrl);
        configMap.put(PULSAR_ADMIN_SERVICE_URL, httpServiceUrl);
        return new RunningDevService(Feature.MESSAGING_PULSAR.getName(), containerId, closeable, configMap);
    }

    private boolean hasPulsarChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isPulsar = connectorValue.equalsIgnoreCase("smallrye-pulsar");
                boolean hasServiceUrl = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".serviceUrl"));
                isConfigured = isPulsar && hasServiceUrl;
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private PulsarDevServiceCfg getConfiguration(PulsarBuildTimeConfig cfg) {
        PulsarDevServicesBuildTimeConfig devServicesConfig = cfg.devservices();
        return new PulsarDevServiceCfg(devServicesConfig);
    }

    private static final class PulsarDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, String> brokerConfig;

        public PulsarDevServiceCfg(PulsarDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled().orElse(true);
            this.imageName = devServicesConfig.imageName();
            this.fixedExposedPort = devServicesConfig.port().orElse(0);
            this.shared = devServicesConfig.shared();
            this.serviceName = devServicesConfig.serviceName();
            this.brokerConfig = devServicesConfig.brokerConfig();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PulsarDevServiceCfg that = (PulsarDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && Objects.equals(brokerConfig, that.brokerConfig);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort, brokerConfig);
        }
    }

}
