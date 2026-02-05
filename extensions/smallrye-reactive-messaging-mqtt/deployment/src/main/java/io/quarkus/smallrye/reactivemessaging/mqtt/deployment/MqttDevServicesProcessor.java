package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
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
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a Mosquitto broker as dev service if needed.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class MqttDevServicesProcessor {

    private static final Logger log = Logger.getLogger(MqttDevServicesProcessor.class);

    /**
     * Label to add to shared Dev Service for MQTT running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-mqtt";

    private static final int MQTT_PORT = 1883;
    private static final int MQTT_TLS_PORT = 8883;

    private static final ContainerLocator mqttContainerLocator = locateContainerWithLabels(MQTT_PORT, DEV_SERVICE_LABEL);
    public static final String SMALLRYE_MQTT_HOST = "mp.messaging.connector.smallrye-mqtt.host";
    public static final String SMALLRYE_MQTT_PORT = "mp.messaging.connector.smallrye-mqtt.port";

    @BuildStep
    public DevServicesResultBuildItem startMqttDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            MqttBuildTimeConfig mqttBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {

        MqttDevServicesBuildTimeConfig config = mqttBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, config.enabled().orElse(true))) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        // Handle ComposeLocator differently - MQTT has custom port detection logic

        return mqttContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .feature(Feature.MESSAGING_MQTT)
                        .containerId(containerAddress.getId())
                        .config(Map.of(
                                "mp.messaging.connector.smallrye-mqtt.host", containerAddress.getHost(),
                                "mp.messaging.connector.smallrye-mqtt.port", String.valueOf(containerAddress.getPort())))
                        .build())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.imageName(), "hivemq", "eclipse-mosquitto"),
                        launchMode.getLaunchMode()).stream()
                        .filter(r -> Arrays.stream(r.containerInfo().exposedPorts())
                                .anyMatch(c -> c.privatePort() == MQTT_PORT || c.privatePort() == MQTT_TLS_PORT))
                        .findFirst()
                        .map(r -> {
                            String host = useSharedNetwork ? ComposeLocator.getServiceName(r)
                                    : DockerClientFactory.instance().dockerHostIpAddress();
                            int port = useSharedNetwork ? MQTT_PORT
                                    : r.getPortMapping(MQTT_PORT).or(() -> r.getPortMapping(MQTT_TLS_PORT)).orElse(0);
                            return DevServicesResultBuildItem.discovered()
                                    .feature(Feature.MESSAGING_MQTT)
                                    .containerId(r.containerInfo().id())
                                    .config(Map.of(
                                            SMALLRYE_MQTT_HOST, host,
                                            SMALLRYE_MQTT_PORT, String.valueOf(port)))
                                    .build();
                        }))
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.MESSAGING_MQTT)
                        .serviceConfig(config)
                        .startable(() -> new ConfiguredMqttContainer(
                                DockerImageName.parse(config.imageName()).asCompatibleSubstituteFor("mqtt"),
                                config.port().orElse(0),
                                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName() : null,
                                compose.getDefaultNetworkId(),
                                useSharedNetwork)
                                .withEnv(config.containerEnv()))
                        .postStartHook(this::logStarted)
                        .configProvider(Map.of(
                                SMALLRYE_MQTT_HOST, ConfiguredMqttContainer::getEffectiveHost,
                                SMALLRYE_MQTT_PORT, s -> String.valueOf(s.getPort())))
                        .build());
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, boolean devServicesEnabled) {
        if (!devServicesEnabled) {
            log.debug("Not starting Dev Services for MQTT, as it has been disabled in the config.");
            return true;
        }

        if (!hasMqttChannelWithoutHostAndPort()) {
            log.debug("Not starting Dev Services for MQTT, all the channels are configured.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, please configure the MQTT broker location.");
            return true;
        }

        return false;
    }

    private void logStarted(ConfiguredMqttContainer container) {
        log.info("Dev Services for MQTT started.");
    }

    private boolean hasMqttChannelWithoutHostAndPort() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            if ((isIncoming || isOutgoing) && isConnector) {
                String connectorValue = config.getValue(name, String.class);
                boolean isMqtt = connectorValue.equalsIgnoreCase("smallrye-mqtt");
                boolean hasHost = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".host"));
                boolean hasPort = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".port"));
                boolean isConfigured = hasHost || hasPort;
                if (isMqtt && !isConfigured) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Container configuring and starting the Mosquitto broker.
     */
    private static final class ConfiguredMqttContainer extends GenericContainer<ConfiguredMqttContainer> implements Startable {

        private final int port;
        private final boolean useSharedNetwork;
        private final String hostName;

        private ConfiguredMqttContainer(
                DockerImageName dockerImageName,
                int fixedExposedPort,
                String serviceName,
                String defaultNetworkId,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            withExposedPorts(MQTT_PORT);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            withClasspathResourceMapping("mosquitto.conf",
                    "/mosquitto/config/mosquitto.conf",
                    BindMode.READ_ONLY);
            if (!dockerImageName.getRepository().endsWith("eclipse-mosquitto")) {
                throw new IllegalArgumentException("Only official eclipse-mosquitto images are supported");
            }
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "mqtt");
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, MQTT_PORT);
            }
        }

        @Override
        public String getConnectionInfo() {
            return String.format("mqtt://%s:%d", getEffectiveHost(), getPort());
        }

        @Override
        public void close() {
            super.close();
        }

        public String getEffectiveHost() {
            return useSharedNetwork ? hostName : getHost();
        }

        public int getPort() {
            return useSharedNetwork ? MQTT_PORT : getMappedPort(MQTT_PORT);
        }
    }
}
