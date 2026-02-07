package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.RabbitMQContainer;
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
 * Starts a RabbitMQ broker as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class RabbitMQDevServicesProcessor {

    private static final Logger log = Logger.getLogger(RabbitMQDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for RabbitMQ running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-rabbitmq";

    private static final int RABBITMQ_PORT = 5672;
    private static final int RABBITMQ_HTTP_PORT = 15672;

    private static final ContainerLocator rabbitmqContainerLocator = locateContainerWithLabels(RABBITMQ_PORT,
            DEV_SERVICE_LABEL);

    private static final String RABBITMQ_HOST_PROP = "rabbitmq-host";
    private static final String RABBITMQ_PORT_PROP = "rabbitmq-port";
    private static final String RABBITMQ_HTTP_PORT_PROP = "rabbitmq-http-port";
    private static final String RABBITMQ_USERNAME_PROP = "rabbitmq-username";
    private static final String RABBITMQ_PASSWORD_PROP = "rabbitmq-password";
    public static final String RABBITMQ_DEFAULT_USER_PASS = "guest";

    @BuildStep
    public DevServicesResultBuildItem startRabbitMQDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            RabbitMQBuildTimeConfig rabbitmqBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {

        RabbitMQDevServicesBuildTimeConfig config = rabbitmqBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, config.enabled().orElse(true))) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        return rabbitmqContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.imageName().orElse(getDefaultImageNameFor("rabbitmq")), "rabbitmq"),
                        RABBITMQ_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> {
                    // Discovered service path
                    RunningContainer container = containerAddress.getRunningContainer();
                    if (container == null) {
                        return null;
                    }
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.MESSAGING_RABBITMQ)
                            .containerId(containerAddress.getId())
                            .config(Map.of(
                                    RABBITMQ_HOST_PROP, containerAddress.getHost(),
                                    RABBITMQ_PORT_PROP, String.valueOf(containerAddress.getPort()),
                                    RABBITMQ_HTTP_PORT_PROP,
                                    String.valueOf(container.getPortMapping(RABBITMQ_HTTP_PORT).orElse(0)),
                                    RABBITMQ_USERNAME_PROP,
                                    container.tryGetEnv("RABBITMQ_DEFAULT_USER").orElse(RABBITMQ_DEFAULT_USER_PASS),
                                    RABBITMQ_PASSWORD_PROP,
                                    container.tryGetEnv("RABBITMQ_DEFAULT_PASS").orElse(RABBITMQ_DEFAULT_USER_PASS)))
                            .build();
                })
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.MESSAGING_RABBITMQ)
                        .serviceConfig(config)
                        .startable(() -> {
                            ConfiguredRabbitMQContainer container = new ConfiguredRabbitMQContainer(
                                    DockerImageName.parse(config.imageName().orElse(getDefaultImageNameFor("rabbitmq")))
                                            .asCompatibleSubstituteFor("rabbitmq"),
                                    config.port().orElse(0),
                                    config.httpPort().orElse(0),
                                    launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName() : null,
                                    compose.getDefaultNetworkId(),
                                    useSharedNetwork);
                            withTopology(config, container);
                            container.withEnv(config.containerEnv());
                            return container;
                        })
                        .postStartHook(this::logStarted)
                        .config(Map.of(
                                RABBITMQ_USERNAME_PROP, RABBITMQ_DEFAULT_USER_PASS,
                                RABBITMQ_PASSWORD_PROP, RABBITMQ_DEFAULT_USER_PASS))
                        .configProvider(Map.of(
                                RABBITMQ_HOST_PROP, ConfiguredRabbitMQContainer::getEffectiveHost,
                                RABBITMQ_PORT_PROP, s -> String.valueOf(s.getPort()),
                                RABBITMQ_HTTP_PORT_PROP, s -> String.valueOf(s.getHttpPort())))
                        .build());
    }

    private ConfiguredRabbitMQContainer withTopology(RabbitMQDevServicesBuildTimeConfig config,
            ConfiguredRabbitMQContainer container) {
        config.vhosts().ifPresent(s -> s.forEach(container::withVhost));
        config.exchanges().forEach((key, ex) -> container.withExchange(ex.vhost(), key, ex.type(), ex.autoDelete(), false,
                ex.durable(), reMapArgs(ex.arguments())));
        config.queues().forEach((key, queue) -> container.withQueue(queue.vhost(), key, queue.autoDelete(), queue.durable(),
                reMapArgs(queue.arguments())));
        config.bindings().forEach((key, binding) -> container.withBinding(binding.vhost(), binding.source().orElse(key),
                binding.destination().orElse(key), reMapArgs(binding.arguments()),
                binding.routingKey(), binding.destinationType()));
        return container;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> reMapArgs(Map<String, String> args) {
        return args != null ? (Map<String, Object>) (Map<?, ?>) args : Collections.emptyMap();
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, boolean devServicesEnabled) {
        if (!devServicesEnabled) {
            log.debug("Not starting Dev Services for RabbitMQ, as it has been disabled in the config.");
            return true;
        }

        // Check if rabbitmq-port or rabbitmq-host are set
        if (ConfigUtils.isPropertyNonEmpty(RABBITMQ_HOST_PROP) || ConfigUtils.isPropertyNonEmpty(RABBITMQ_PORT_PROP)) {
            log.debug("Not starting Dev Services for RabbitMQ, the rabbitmq-host and/or rabbitmq-port are configured.");
            return true;
        }

        // Verify that we have RabbitMQ channels without host and port
        if (!hasRabbitMQChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for RabbitMQ, all the channels are configured.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the RabbitMQ broker location.");
            return true;
        }

        return false;
    }

    private void logStarted(ConfiguredRabbitMQContainer container) {
        log.infof("Dev Services for RabbitMQ started. Other Quarkus applications in dev mode will find the "
                + "broker automatically. For Quarkus applications in production mode, you can connect to"
                + " this by starting your application with -Drabbitmq-host=%s -Drabbitmq-port=%s -Drabbitmq-username=%s -Drabbitmq-password=%s",
                container.getEffectiveHost(), container.getPort(), RABBITMQ_DEFAULT_USER_PASS, RABBITMQ_DEFAULT_USER_PASS);
    }

    private boolean hasRabbitMQChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isRabbitMQ = connectorValue.equalsIgnoreCase("smallrye-rabbitmq");
                boolean hasHost = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".port"));
                isConfigured = isRabbitMQ && (hasHost || hasPort);
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    /**
     * Container configuring and starting the RabbitMQ broker.
     */
    private static final class ConfiguredRabbitMQContainer extends RabbitMQContainer implements Startable {

        private final int port;
        private final int httpPort;
        private final boolean useSharedNetwork;
        private final String hostName;

        private ConfiguredRabbitMQContainer(DockerImageName dockerImageName,
                int fixedExposedPort, int fixedExposedHttpPort,
                String serviceName, String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.httpPort = fixedExposedHttpPort;
            this.useSharedNetwork = useSharedNetwork;
            withExposedPorts(RABBITMQ_PORT, RABBITMQ_HTTP_PORT);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            if (!dockerImageName.getRepository().endsWith("rabbitmq")) {
                throw new IllegalArgumentException("Only official rabbitmq images are supported");
            }
            hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "rabbitmq");
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, RABBITMQ_PORT);
            }
            if (httpPort > 0) {
                addFixedExposedPort(httpPort, RABBITMQ_HTTP_PORT);
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

        public String getEffectiveHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        public int getPort() {
            return useSharedNetwork ? RABBITMQ_PORT : getMappedPort(RABBITMQ_PORT);
        }

        @Override
        public Integer getHttpPort() {
            return useSharedNetwork ? RABBITMQ_HTTP_PORT : getMappedPort(RABBITMQ_HTTP_PORT);
        }
    }
}
