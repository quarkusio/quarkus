package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
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
 * Starts a Mosquitto broker as dev service if needed.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class MqttDevServicesProcessor {

    private static final Logger log = Logger.getLogger(MqttDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for MQTT running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-mqtt";

    private static final int MQTT_PORT = 1883;

    private static final ContainerLocator mqttContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, MQTT_PORT);
    static volatile RunningDevService devService;
    static volatile MqttDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startMqttDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            MqttBuildTimeConfig mqttClientBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        MqttDevServiceCfg configuration = getConfiguration(mqttClientBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "MQTT Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            RunningDevService newDevService = startMqttBroker(dockerStatusBuildItem, configuration, launchMode,
                    devServicesConfig.timeout);
            if (newDevService != null) {
                devService = newDevService;

                Map<String, String> config = devService.getConfig();
                if (devService.isOwner()) {
                    log.info("Dev Services for MQTT started.");
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

                    log.info("Dev Services for MQTT shut down.");
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
                log.error("Failed to stop the MQTT broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startMqttBroker(DockerStatusBuildItem dockerStatusBuildItem,
            MqttDevServiceCfg config, LaunchModeBuildItem launchMode,
            Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Dev Services for MQTT, as it has been disabled in the config.");
            return null;
        }

        // Verify that we have MQTT channels without host and port
        if (!hasMqttChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for MQTT, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Docker isn't working, please configure the MQTT broker location.");
            return null;
        }

        ConfiguredMqttContainer container = new ConfiguredMqttContainer(
                DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("mqtt"),
                config.fixedExposedPort,
                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null);

        final Supplier<RunningDevService> defaultMqttBrokerSupplier = () -> {

            // Starting the broker
            timeout.ifPresent(container::withStartupTimeout);
            container.withEnv(config.containerEnv);
            container.start();
            return getRunningDevService(
                    container.getContainerId(),
                    container::close,
                    container.getHost(),
                    container.getPort());
        };

        return mqttContainerLocator
                .locateContainer(
                        config.serviceName,
                        config.shared,
                        launchMode.getLaunchMode())
                .map(containerAddress -> getRunningDevService(
                        containerAddress.getId(),
                        null,
                        containerAddress.getHost(),
                        containerAddress.getPort()))
                .orElseGet(defaultMqttBrokerSupplier);
    }

    private RunningDevService getRunningDevService(
            String containerId,
            Closeable closeable,
            String host,
            int port) {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mp.messaging.connector.smallrye-mqtt.host", host);
        configMap.put("mp.messaging.connector.smallrye-mqtt.port", String.valueOf(port));
        return new RunningDevService(Feature.SMALLRYE_REACTIVE_MESSAGING_MQTT.getName(),
                containerId, closeable, configMap);
    }

    private boolean hasMqttChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isMqtt = connectorValue.equalsIgnoreCase("smallrye-mqtt");
                boolean hasHost = ConfigUtils.isPropertyPresent(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyPresent(name.replace(".connector", ".port"));
                isConfigured = isMqtt && (hasHost || hasPort);
            }

            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private MqttDevServiceCfg getConfiguration(MqttBuildTimeConfig cfg) {
        MqttDevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new MqttDevServiceCfg(devServicesConfig);
    }

    private static final class MqttDevServiceCfg {

        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, String> containerEnv;

        public MqttDevServiceCfg(MqttDevServicesBuildTimeConfig devServicesConfig) {
            this.devServicesEnabled = devServicesConfig.enabled.orElse(true);
            this.imageName = devServicesConfig.imageName;
            this.fixedExposedPort = devServicesConfig.port.orElse(0);
            this.shared = devServicesConfig.shared;
            this.serviceName = devServicesConfig.serviceName;
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
            MqttDevServiceCfg that = (MqttDevServiceCfg) o;
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
     * Container configuring and starting the Mosquitto broker.
     */
    private static final class ConfiguredMqttContainer extends GenericContainer<ConfiguredMqttContainer> {

        private final int port;

        private ConfiguredMqttContainer(
                DockerImageName dockerImageName,
                int fixedExposedPort,
                String serviceName) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            withExposedPorts(MQTT_PORT);
            withNetwork(Network.SHARED);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            withClasspathResourceMapping("mosquitto.conf",
                    "/mosquitto/config/mosquitto.conf",
                    BindMode.READ_ONLY);
            if (!dockerImageName.getRepository().endsWith("eclipse-mosquitto")) {
                throw new IllegalArgumentException("Only official eclipse-mosquitto images are supported");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, MQTT_PORT);
            }
        }

        public int getPort() {
            return getMappedPort(MQTT_PORT);
        }
    }
}
