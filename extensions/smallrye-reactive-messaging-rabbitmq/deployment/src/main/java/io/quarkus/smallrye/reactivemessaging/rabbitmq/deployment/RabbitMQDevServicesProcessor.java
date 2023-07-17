package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a RabbitMQ broker as dev service if needed.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class RabbitMQDevServicesProcessor {

    private static final Logger log = Logger.getLogger(RabbitMQDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for RabbitMQ running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-rabbitmq";

    private static final int RABBITMQ_PORT = 5672;
    private static final int RABBITMQ_HTTP_PORT = 15672;

    private static final ContainerLocator rabbitmqContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, RABBITMQ_PORT);
    private static final String RABBITMQ_HOST_PROP = "rabbitmq-host";
    private static final String RABBITMQ_PORT_PROP = "rabbitmq-port";
    private static final String RABBITMQ_HTTP_PORT_PROP = "rabbitmq-http-port";
    private static final String RABBITMQ_USERNAME_PROP = "rabbitmq-username";
    private static final String RABBITMQ_PASSWORD_PROP = "rabbitmq-password";

    static volatile RunningDevService devService;
    static volatile RabbitMQDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startRabbitMQDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            RabbitMQBuildTimeConfig rabbitmqClientBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        RabbitMQDevServiceCfg configuration = getConfiguration(rabbitmqClientBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "RabbitMQ Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startRabbitMQBroker(dockerStatusBuildItem, configuration, launchMode,
                    devServicesConfig.timeout);
            if (newDevService != null) {
                devService = newDevService;

                Map<String, String> config = devService.getConfig();
                if (devService.isOwner()) {
                    log.info("Dev Services for RabbitMQ started.");
                    log.infof("Other Quarkus applications in dev mode will find the "
                            + "broker automatically. For Quarkus applications in production mode, you can connect to"
                            + " this by starting your application with -Drabbitmq-host=%s -Drabbitmq-port=%s -Drabbitmq-username=%s -Drabbitmq-password=%s",
                            config.get(RABBITMQ_HOST_PROP), config.get(RABBITMQ_PORT_PROP),
                            config.get(RABBITMQ_USERNAME_PROP), config.get(RABBITMQ_PASSWORD_PROP));
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

                    log.info("Dev Services for RabbitMQ shut down.");
                }
                first = true;
                devService = null;
                cfg = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        }
        cfg = configuration;
        return devService.toBuildItem();
    }

    private void shutdownBroker() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the RabbitMQ broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startRabbitMQBroker(DockerStatusBuildItem dockerStatusBuildItem,
            RabbitMQDevServiceCfg config, LaunchModeBuildItem launchMode,
            Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for RabbitMQ, as it has been disabled in the config.");
            return null;
        }

        // Check if rabbitmq-port or rabbitmq-host are set
        if (ConfigUtils.isPropertyPresent(RABBITMQ_HOST_PROP) || ConfigUtils.isPropertyPresent(RABBITMQ_PORT_PROP)) {
            log.debug("Not starting Dev Services for RabbitMQ, the rabbitmq-host and/or rabbitmq-port are configured.");
            return null;
        }

        // Verify that we have RabbitMQ channels without host and port
        if (!hasRabbitMQChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for RabbitMQ, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Docker isn't working, please configure the RabbitMQ broker location.");
            return null;
        }

        ConfiguredRabbitMQContainer container = new ConfiguredRabbitMQContainer(
                DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("rabbitmq"),
                config.fixedExposedPort,
                config.fixedExposedHttpPort,
                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null);

        config.exchanges.forEach(x -> container.withExchange(x.name, x.type, x.autoDelete, false, x.durable, x.arguments));
        config.queues.forEach(x -> container.withQueue(x.name, x.autoDelete, x.durable, x.arguments));
        config.bindings
                .forEach(b -> container.withBinding(b.source, b.destination, b.arguments, b.routingKey, b.destinationType));

        final Supplier<RunningDevService> defaultRabbitMQBrokerSupplier = () -> {

            // Starting the broker
            timeout.ifPresent(container::withStartupTimeout);
            container.withEnv(config.containerEnv);
            container.start();
            return getRunningDevService(container.getContainerId(), container::close, container.getHost(),
                    container.getPort(), container.getHttpPort(), container.getAdminUsername(), container.getAdminPassword());
        };

        Integer httpPort = rabbitmqContainerLocator
                .locatePublicPort(config.serviceName, config.shared, launchMode.getLaunchMode(), RABBITMQ_HTTP_PORT)
                .orElse(0);

        return rabbitmqContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> getRunningDevService(containerAddress.getId(), null, containerAddress.getHost(),
                        containerAddress.getPort(), httpPort, container.getAdminUsername(),
                        container.getAdminPassword()))
                .orElseGet(defaultRabbitMQBrokerSupplier);
    }

    private RunningDevService getRunningDevService(String containerId, Closeable closeable, String host, int port, int httpPort,
            String username, String password) {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(RABBITMQ_HOST_PROP, host);
        configMap.put(RABBITMQ_PORT_PROP, String.valueOf(port));
        configMap.put(RABBITMQ_HTTP_PORT_PROP, String.valueOf(httpPort));
        configMap.put(RABBITMQ_USERNAME_PROP, username);
        configMap.put(RABBITMQ_PASSWORD_PROP, password);
        return new RunningDevService(Feature.SMALLRYE_REACTIVE_MESSAGING_RABBITMQ.getName(),
                containerId, closeable, configMap);
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
                boolean hasHost = ConfigUtils.isPropertyPresent(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyPresent(name.replace(".connector", ".port"));
                isConfigured = isRabbitMQ && (hasHost || hasPort);
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private RabbitMQDevServiceCfg getConfiguration(RabbitMQBuildTimeConfig cfg) {
        RabbitMQDevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new RabbitMQDevServiceCfg(devServicesConfig);
    }

    private static final class RabbitMQDevServiceCfg {

        static class Exchange {
            String name;
            String type;
            Boolean autoDelete;
            Boolean durable;
            Map<String, Object> arguments;

            Exchange(Map.Entry<String, RabbitMQDevServicesBuildTimeConfig.Exchange> entry) {
                this(entry.getKey(), entry.getValue());
            }

            Exchange(String name, RabbitMQDevServicesBuildTimeConfig.Exchange source) {
                this.name = name;
                this.type = source.type;
                this.autoDelete = source.autoDelete;
                this.durable = source.durable;
                this.arguments = source.arguments != null
                        ? source.arguments.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))
                        : Map.of();
            }
        }

        static class Queue {
            String name;
            Boolean autoDelete;
            Boolean durable;
            Map<String, Object> arguments;

            Queue(Map.Entry<String, RabbitMQDevServicesBuildTimeConfig.Queue> entry) {
                this(entry.getKey(), entry.getValue());
            }

            Queue(String name, RabbitMQDevServicesBuildTimeConfig.Queue source) {
                this.name = name;
                this.autoDelete = source.autoDelete;
                this.durable = source.durable;
                this.arguments = source.arguments != null
                        ? source.arguments.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))
                        : Map.of();
            }
        }

        static class Binding {
            String source;
            String destination;
            String routingKey;
            String destinationType;
            Map<String, Object> arguments;

            Binding(Map.Entry<String, RabbitMQDevServicesBuildTimeConfig.Binding> entry) {
                this(entry.getKey(), entry.getValue());
            }

            Binding(String name, RabbitMQDevServicesBuildTimeConfig.Binding source) {
                this.source = source.source.orElse(name);
                this.routingKey = source.routingKey;
                this.destination = source.destination.orElse(name);
                this.destinationType = source.destinationType;
                this.arguments = source.arguments != null
                        ? source.arguments.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))
                        : Map.of();
            }
        }

        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final Integer fixedExposedHttpPort;
        private final boolean shared;
        private final String serviceName;
        private final List<Exchange> exchanges;
        private final List<Queue> queues;
        private final List<Binding> bindings;
        private final Map<String, String> containerEnv;

        public RabbitMQDevServiceCfg(RabbitMQDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled.orElse(true);
            this.imageName = devServicesConfig.imageName;
            this.fixedExposedPort = devServicesConfig.port.orElse(0);
            this.fixedExposedHttpPort = devServicesConfig.httpPort.orElse(0);
            this.shared = devServicesConfig.shared;
            this.serviceName = devServicesConfig.serviceName;
            this.exchanges = devServicesConfig.exchanges != null
                    ? devServicesConfig.exchanges.entrySet().stream().map(Exchange::new).collect(Collectors.toList())
                    : Collections.emptyList();
            this.queues = devServicesConfig.queues != null
                    ? devServicesConfig.queues.entrySet().stream().map(Queue::new).collect(Collectors.toList())
                    : Collections.emptyList();
            this.bindings = devServicesConfig.bindings != null
                    ? devServicesConfig.bindings.entrySet().stream().map(Binding::new).collect(Collectors.toList())
                    : Collections.emptyList();
            this.containerEnv = devServicesConfig.containerEnv;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RabbitMQDevServiceCfg that = (RabbitMQDevServiceCfg) o;
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
    private static final class ConfiguredRabbitMQContainer extends RabbitMQContainer {

        private final int port;
        private final int httpPort;

        private ConfiguredRabbitMQContainer(DockerImageName dockerImageName, int fixedExposedPort, int fixedExposedHttpPort,
                String serviceName) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.httpPort = fixedExposedHttpPort;
            withNetwork(Network.SHARED);
            withExposedPorts(RABBITMQ_PORT, RABBITMQ_HTTP_PORT);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            if (!dockerImageName.getRepository().endsWith("rabbitmq")) {
                throw new IllegalArgumentException("Only official rabbitmq images are supported");
            }
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

        public int getPort() {
            return getMappedPort(RABBITMQ_PORT);
        }
    }
}
