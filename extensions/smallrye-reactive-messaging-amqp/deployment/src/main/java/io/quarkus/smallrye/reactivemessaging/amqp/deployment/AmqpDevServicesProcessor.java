package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a AMQP 1.0 broker as dev service if needed.
 * It uses <a href="https://quay.io/repository/artemiscloud/activemq-artemis-broker">activemq-artemis-broker</a> as image.
 * See <a href="https://artemiscloud.io/">Artemis Cloud</a> for details.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class AmqpDevServicesProcessor {

    private static final Logger log = Logger.getLogger(AmqpDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for AMQP running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-amqp";

    private static final int AMQP_PORT = 5672;
    private static final int AMQP_CONSOLE_PORT = 8161;

    private static final ContainerLocator amqpContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, AMQP_PORT);
    private static final String AMQP_HOST_PROP = "amqp-host";
    private static final String AMQP_PORT_PROP = "amqp-port";
    private static final String AMQP_MAPPED_PORT_PROP = "amqp-mapped-port";
    private static final String AMQP_USER_PROP = "amqp-user";
    private static final String AMQP_PASSWORD_PROP = "amqp-password";

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    static volatile RunningDevService devService;
    static volatile AmqpDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startAmqpDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            AmqpBuildTimeConfig amqpClientBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem) {

        AmqpDevServiceCfg configuration = getConfiguration(amqpClientBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "AMQP Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startAmqpBroker(dockerStatusBuildItem, configuration, launchMode,
                    devServicesConfig.timeout(), !devServicesSharedNetworkBuildItem.isEmpty());
            if (newDevService != null) {
                devService = newDevService;
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

                    log.info("Dev Services for AMQP shut down.");
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        if (devService.isOwner()) {
            Map<String, String> config = devService.getConfig();
            log.infof("Dev Services for AMQP started. Other Quarkus applications in dev mode will find the "
                    + "broker automatically. For Quarkus applications in production mode, you can connect to"
                    + " this by starting your application with -Damqp.host=%s -Damqp.port=%s -Damqp.user=%s -Damqp.password=%s",
                    config.get(AMQP_HOST_PROP), config.get(AMQP_PORT_PROP), config.get(AMQP_USER_PROP),
                    config.get(AMQP_PASSWORD_PROP));
        }

        return devService.toBuildItem();
    }

    private void shutdownBroker() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the AMQP broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startAmqpBroker(DockerStatusBuildItem dockerStatusBuildItem, AmqpDevServiceCfg config,
            LaunchModeBuildItem launchMode, Optional<Duration> timeout, boolean useSharedNetwork) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for AMQP, as it has been disabled in the config.");
            return null;
        }

        // Check if amqp.port or amqp.host are set
        if (ConfigUtils.isPropertyNonEmpty(AMQP_HOST_PROP) || ConfigUtils.isPropertyNonEmpty(AMQP_PORT_PROP)) {
            log.debug("Not starting Dev Services for AMQP, the amqp.host and/or amqp.port are configured.");
            return null;
        }

        // Verify that we have AMQP channels without host and port
        if (!hasAmqpChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for AMQP, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the AMQP broker location.");
            return null;
        }

        final Supplier<RunningDevService> defaultAmqpBrokerSupplier = () -> {
            // Starting the broker
            ArtemisContainer container = new ArtemisContainer(
                    DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("artemiscloud/activemq-artemis-broker"),
                    config.extra,
                    config.fixedExposedPort,
                    launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                    useSharedNetwork);

            timeout.ifPresent(container::withStartupTimeout);
            container.withEnv(config.containerEnv);
            container.start();

            return getRunningService(container.getContainerId(), container::close, container.getEffectiveHost(),
                    container.getPort(), container.getMappedPort());
        };

        return amqpContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> getRunningService(containerAddress.getId(), null, containerAddress.getHost(),
                        containerAddress.getPort(), 0))
                .orElseGet(defaultAmqpBrokerSupplier);
    }

    private RunningDevService getRunningService(String containerId, Closeable closeable, String host, int port,
            int mappedPort) {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(AMQP_HOST_PROP, host);
        configMap.put(AMQP_PORT_PROP, String.valueOf(port));
        configMap.put(AMQP_MAPPED_PORT_PROP, String.valueOf(mappedPort));
        configMap.put(AMQP_USER_PROP, DEFAULT_USER);
        configMap.put(AMQP_PASSWORD_PROP, DEFAULT_PASSWORD);
        return new RunningDevService(Feature.MESSAGING_AMQP.getName(), containerId, closeable, configMap);
    }

    private boolean hasAmqpChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isAmqp = connectorValue.equalsIgnoreCase("smallrye-amqp");
                boolean hasHost = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".port"));
                isConfigured = isAmqp && (hasHost || hasPort);
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private AmqpDevServiceCfg getConfiguration(AmqpBuildTimeConfig cfg) {
        AmqpDevServicesBuildTimeConfig devServicesConfig = cfg.devservices();
        return new AmqpDevServiceCfg(devServicesConfig);
    }

    private static final class AmqpDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final String extra;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, String> containerEnv;

        public AmqpDevServiceCfg(AmqpDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled().orElse(true);
            this.imageName = devServicesConfig.imageName();
            this.fixedExposedPort = devServicesConfig.port().orElse(0);
            this.extra = devServicesConfig.extraArgs();
            this.shared = devServicesConfig.shared();
            this.serviceName = devServicesConfig.serviceName();
            this.containerEnv = devServicesConfig.containerEnv();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AmqpDevServiceCfg that = (AmqpDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && Objects.equals(containerEnv, that.containerEnv);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort, containerEnv);
        }
    }

    /**
     * Container configuring and starting the Artemis broker.
     */
    private static final class ArtemisContainer extends GenericContainer<ArtemisContainer> {

        private final int port;
        private final boolean useSharedNetwork;

        private String hostName;

        private ArtemisContainer(DockerImageName dockerImageName, String extra, int fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            withExposedPorts(AMQP_PORT, AMQP_CONSOLE_PORT);
            withEnv("AMQ_USER", DEFAULT_USER);
            withEnv("AMQ_PASSWORD", DEFAULT_PASSWORD);
            withEnv("AMQ_EXTRA_ARGS", extra);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            if (dockerImageName.getRepository().endsWith("artemiscloud/activemq-artemis-broker")) {
                waitingFor(Wait.forLogMessage(".*AMQ241004.*", 1)); // Artemis console available.
            } else {
                log.infof(
                        "Detected a different image (%s) for the Dev Service for AMQP. Ensure it's compatible with artemiscloud/activemq-artemis-broker. "
                                +
                                "Refer to https://quarkus.io/guides/amqp-dev-services#configuring-the-image for details.",
                        dockerImageName);
                log.info("Skipping startup probe for the Dev Service for AMQP as it does not use the default image.");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, AMQP_PORT);
            }

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "artemis");
            } else {
                hostName = super.getHost();
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return AMQP_PORT;
            }

            return getMappedPort(AMQP_PORT);
        }

        public String getEffectiveHost() {
            return hostName;
        }

        public int getMappedPort() {
            return getMappedPort(AMQP_PORT);
        }
    }
}
