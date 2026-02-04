package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a AMQP 1.0 broker as dev service if needed.
 * It uses <a href="https://quay.io/repository/artemiscloud/activemq-artemis-broker">activemq-artemis-broker</a> as image.
 * See <a href="https://artemiscloud.io/">Artemis Cloud</a> for details.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class AmqpDevServicesProcessor {

    private static final Logger log = Logger.getLogger(AmqpDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for AMQP running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-amqp";

    private static final int AMQP_PORT = 5672;
    private static final int AMQP_CONSOLE_PORT = 8161;

    private static final ContainerLocator amqpContainerLocator = locateContainerWithLabels(AMQP_PORT, DEV_SERVICE_LABEL);
    private static final String AMQP_HOST_PROP = "amqp-host";
    private static final String AMQP_PORT_PROP = "amqp-port";
    private static final String AMQP_MAPPED_PORT_PROP = "amqp-mapped-port";
    private static final String AMQP_USER_PROP = "amqp-username";
    private static final String AMQP_PASSWORD_PROP = "amqp-password";

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    @BuildStep
    public DevServicesResultBuildItem startAmqpDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            AmqpBuildTimeConfig amqpBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {

        AmqpDevServicesBuildTimeConfig config = amqpBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, config.enabled().orElse(true))) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        return amqpContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.imageName().orElse(getDefaultImageNameFor("amqp")),
                                "amqp", "activemq-artemis", "amq-broker", "rabbitmq"),
                        AMQP_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> {
                    // Discovered service
                    RunningContainer container = containerAddress.getRunningContainer();
                    if (container == null) {
                        return null;
                    }
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.MESSAGING_AMQP)
                            .containerId(containerAddress.getId())
                            .config(Map.of(
                                    AMQP_HOST_PROP, containerAddress.getHost(),
                                    AMQP_PORT_PROP, String.valueOf(containerAddress.getPort()),
                                    AMQP_MAPPED_PORT_PROP,
                                    String.valueOf(container.getPortMapping(AMQP_CONSOLE_PORT).orElse(0)),
                                    AMQP_USER_PROP, container.tryGetEnv("AMQP_USER", "ARTEMIS_USER", "RABBITMQ_DEFAULT_USER")
                                            .orElse(DEFAULT_USER),
                                    AMQP_PASSWORD_PROP,
                                    container.tryGetEnv("AMQP_PASSWORD", "ARTEMIS_PASSWORD", "RABBITMQ_DEFAULT_PASS")
                                            .orElse(DEFAULT_PASSWORD)))
                            .build();
                })
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.MESSAGING_AMQP)
                        .serviceConfig(config)
                        .startable(() -> new ArtemisContainer(
                                DockerImageName.parse(config.imageName().orElse(getDefaultImageNameFor("amqp")))
                                        .asCompatibleSubstituteFor("artemiscloud/activemq-artemis-broker"),
                                config.extraArgs(),
                                config.port().orElse(0),
                                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName() : null,
                                compose.getDefaultNetworkId(),
                                useSharedNetwork)
                                .withEnv(config.containerEnv())
                                .withSharedServiceLabel(launchMode.getLaunchMode(), config.serviceName()))
                        .postStartHook(this::logStarted)
                        .config(Map.of(
                                AMQP_USER_PROP, DEFAULT_USER,
                                AMQP_PASSWORD_PROP, DEFAULT_PASSWORD))
                        .configProvider(Map.of(
                                AMQP_HOST_PROP, ArtemisContainer::getEffectiveHost,
                                AMQP_PORT_PROP, s -> String.valueOf(s.getPort()),
                                AMQP_MAPPED_PORT_PROP, s -> String.valueOf(s.getMappedConsolePort())))
                        .build());
    }

    private void logStarted(ArtemisContainer container) {
        log.infof("Dev Services for AMQP started. Other Quarkus applications in dev mode will find the "
                + "broker automatically. For Quarkus applications in production mode, you can connect to"
                + " this by starting your application with -Damqp-host=%s -Damqp-port=%s -Damqp-username=%s -Damqp-password=%s",
                container.getEffectiveHost(), container.getPort(), DEFAULT_USER, DEFAULT_PASSWORD);
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

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, boolean devServicesEnabled) {
        if (!devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for AMQP, as it has been disabled in the config.");
            return true;
        }

        // Check if amqp-host or amqp-port are set
        if (ConfigUtils.isPropertyNonEmpty(AMQP_HOST_PROP) || ConfigUtils.isPropertyNonEmpty(AMQP_PORT_PROP)) {
            log.debug("Not starting Dev Services for AMQP, the amqp-host and/or amqp-port are configured.");
            return true;
        }

        // Verify that we have AMQP channels without host and port
        if (!hasAmqpChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for AMQP, all the channels are configured.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the AMQP broker location.");
            return true;
        }

        return false;
    }

    /**
     * Container configuring and starting the Artemis broker.
     */
    private static final class ArtemisContainer extends GenericContainer<ArtemisContainer> implements Startable {

        private final int port;
        private final boolean useSharedNetwork;

        private final String hostName;

        private ArtemisContainer(DockerImageName dockerImageName, String extra, int fixedExposedPort, String serviceName,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            withExposedPorts(AMQP_PORT, AMQP_CONSOLE_PORT);
            withEnv("AMQ_USER", DEFAULT_USER);
            withEnv("AMQ_PASSWORD", DEFAULT_PASSWORD);
            withEnv("AMQ_EXTRA_ARGS", extra);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
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
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "artemis");
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, AMQP_PORT);
            }
        }

        @Override
        public String getConnectionInfo() {
            return String.format("amqp://%s:%d", getEffectiveHost(), getPort());
        }

        @Override
        public void close() {
            super.close();
        }

        public int getPort() {
            return useSharedNetwork ? AMQP_PORT : getMappedPort(AMQP_PORT);
        }

        public String getEffectiveHost() {
            return useSharedNetwork ? hostName : getHost();
        }

        public int getMappedConsolePort() {
            return getMappedPort(AMQP_CONSOLE_PORT);
        }

        public ArtemisContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
            return ConfigureUtil.configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
        }
    }
}
