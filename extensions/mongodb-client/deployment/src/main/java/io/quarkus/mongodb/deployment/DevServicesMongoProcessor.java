package io.quarkus.mongodb.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.mongodb.runtime.MongoConfig.isDefaultClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.message.BasicNameValuePair;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URLEncodedUtils;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
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
import io.quarkus.devservices.common.Labels;
import io.quarkus.mongodb.deployment.spi.MongoClientBuildItem;
import io.quarkus.mongodb.deployment.spi.MongoClientsBuildItem;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesMongoProcessor {

    private static final Logger log = Logger.getLogger(DevServicesMongoProcessor.class);

    private static final String MONGO_SCHEME = "mongodb://";

    private static final int MONGO_EXPOSED_PORT = 27017;

    /**
     * Label to add to shared Dev Service for Mongo running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-mongodb";

    private static final ContainerLocator MONGO_CONTAINER_LOCATOR = locateContainerWithLabels(MONGO_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    @BuildStep
    public void startMongo(
            MongoClientsBuildItem mongoClients,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {

        List<String> connectionNames = new ArrayList<>(mongoClients.getMongoClients().size());
        for (MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            connectionNames.add(mongoClient.getName());
        }

        for (String connectionName : connectionNames) {
            CapturedProperties captured = captureProperties(connectionName, mongoClientBuildTimeConfig);

            if (!captured.devServicesEnabled) {
                log.debug(
                        "Not starting devservices for "
                                + (isDefaultClient(connectionName) ? "default datasource" : connectionName)
                                + " as it has been disabled in the config");
                continue;
            }

            String configPrefix = getConfigPrefix(connectionName);

            boolean needToStart = !ConfigUtils.isPropertyNonEmpty(configPrefix + "connection-string")
                    && !ConfigUtils.isPropertyNonEmpty(configPrefix + "hosts");
            if (!needToStart) {
                log.debug(
                        "Not starting devservices for "
                                + (isDefaultClient(connectionName) ? "default datasource" : connectionName)
                                + " as a connection string and/or server addresses have been provided");
                continue;
            }

            if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
                log.warn("Please configure datasource URL for "
                        + (isDefaultClient(connectionName) ? "default datasource" : connectionName)
                        + " or get a working docker instance");
                continue;
            }

            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);

            DevServicesResultBuildItem discovered = discoverRunningService(composeProjectBuildItem, connectionName,
                    captured, launchMode.getLaunchMode(), useSharedNetwork);
            if (discovered != null) {
                devServicesResult.produce(discovered);
            } else {
                String serviceName = mongoClientBuildTimeConfig.devservices().serviceName();
                devServicesResult.produce(
                        DevServicesResultBuildItem.owned()
                                .feature(Feature.MONGODB_CLIENT)
                                .serviceName(connectionName)
                                .serviceConfig(captured)
                                .startable(() -> createMongoContainer(captured, composeProjectBuildItem,
                                        useSharedNetwork, devServicesConfig.timeout(), launchMode.getLaunchMode(),
                                        serviceName))
                                .configProvider(Map.of(
                                        configPrefix + "connection-string",
                                        (Function<QuarkusMongoDBContainer, String>) container -> getEffectiveUrl(
                                                configPrefix, container, captured)))
                                .build());
            }
        }
    }

    private DevServicesResultBuildItem discoverRunningService(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            String connectionName, CapturedProperties captured, LaunchMode launchMode, boolean useSharedNetwork) {
        String configPrefix = getConfigPrefix(connectionName);
        return MONGO_CONTAINER_LOCATOR
                .locateContainer(captured.serviceName(), captured.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(captured.imageName, "mongo"), MONGO_EXPOSED_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    String effectiveUrl = getEffectiveUrl(configPrefix, containerAddress.getHost(),
                            containerAddress.getPort(), captured);
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.MONGODB_CLIENT)
                            .containerId(containerAddress.getId())
                            .config(Map.of(configPrefix + "connection-string", effectiveUrl))
                            .build();
                })
                .orElse(null);
    }

    private QuarkusMongoDBContainer createMongoContainer(CapturedProperties captured,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork, Optional<Duration> timeout, LaunchMode launchMode, String serviceName) {
        QuarkusMongoDBContainer container;
        if (captured.imageName != null) {
            container = new QuarkusMongoDBContainer(
                    DockerImageName.parse(captured.imageName).asCompatibleSubstituteFor("mongo"),
                    captured.fixedExposedPort,
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork, launchMode, serviceName);
        } else {
            container = new QuarkusMongoDBContainer(captured.fixedExposedPort,
                    composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, launchMode, serviceName);
        }
        timeout.ifPresent(container::withStartupTimeout);
        container.withEnv(captured.containerEnv);
        return container;
    }

    private String getEffectiveUrl(String configPrefix, QuarkusMongoDBContainer container,
            CapturedProperties captured) {
        return getEffectiveUrl(configPrefix, container.getEffectiveHost(), container.getEffectivePort(), captured);
    }

    private String getEffectiveUrl(String configPrefix, String host, int port, CapturedProperties capturedProperties) {
        final String databaseName = ConfigProvider.getConfig()
                .getOptionalValue(configPrefix + "database", String.class)
                .orElse("test");
        String effectiveUrl = String.format("%s%s:%d/%s", MONGO_SCHEME, host, port, databaseName);
        if ((capturedProperties.connectionProperties != null) && !capturedProperties.connectionProperties.isEmpty()) {
            effectiveUrl = effectiveUrl + "?"
                    + URLEncodedUtils.format(
                            capturedProperties.connectionProperties.entrySet().stream()
                                    .map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
                                    .collect(Collectors.toList()),
                            StandardCharsets.UTF_8);
        }
        return effectiveUrl;
    }

    private String getConfigPrefix(String connectionName) {
        String configPrefix = "quarkus." + MongoConfig.CONFIG_NAME + ".";
        if (!isDefaultClient(connectionName)) {
            configPrefix = configPrefix + connectionName + ".";
        }
        return configPrefix;
    }

    private CapturedProperties captureProperties(String connectionName, MongoClientBuildTimeConfig mongoClientBuildTimeConfig) {
        String configPrefix = getConfigPrefix(connectionName);
        String databaseName = ConfigProvider.getConfig().getOptionalValue(configPrefix + "database", String.class).orElse(null);
        String connectionString = ConfigProvider.getConfig().getOptionalValue(configPrefix + "connection-string", String.class)
                .orElse(null);
        //TODO: update for multiple connections
        DevServicesBuildTimeConfig devServicesConfig = mongoClientBuildTimeConfig.devservices();
        boolean devServicesEnabled = devServicesConfig.enabled().orElse(true);
        return new CapturedProperties(databaseName, connectionString, devServicesEnabled,
                devServicesConfig.imageName().orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mongo")),
                devServicesConfig.port().orElse(null), devServicesConfig.properties(), devServicesConfig.containerEnv(),
                devServicesConfig.shared(), devServicesConfig.serviceName());
    }

    private record CapturedProperties(String database, String connectionString, boolean devServicesEnabled,
            String imageName, Integer fixedExposedPort,
            Map<String, String> connectionProperties, Map<String, String> containerEnv,
            boolean shared, String serviceName) {

    }

    private static final class QuarkusMongoDBContainer extends MongoDBContainer implements Startable {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        private static final int MONGODB_INTERNAL_PORT = 27017;

        @SuppressWarnings("deprecation")
        private QuarkusMongoDBContainer(Integer fixedExposedPort, String defaultNetworkId, boolean useSharedNetwork,
                LaunchMode launchMode, String serviceName) {
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "mongo");
            this.withLabel(Labels.QUARKUS_DEV_SERVICE, launchMode == LaunchMode.DEVELOPMENT ? serviceName : null);
        }

        private QuarkusMongoDBContainer(DockerImageName dockerImageName, Integer fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork,
                LaunchMode launchMode, String serviceName) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "mongo");
            this.withLabel(Labels.QUARKUS_DEV_SERVICE, launchMode == LaunchMode.DEVELOPMENT ? serviceName : null);
        }

        @Override
        public void configure() {
            super.configure();
            if (useSharedNetwork) {
                return;
            }

            if (fixedExposedPort != null) {
                addFixedExposedPort(fixedExposedPort, MONGODB_INTERNAL_PORT);
            } else {
                addExposedPort(MONGODB_INTERNAL_PORT);
            }
        }

        @Override
        public String getReplicaSetUrl(String databaseName) {
            if (useSharedNetwork) {
                if (!isRunning()) { // done by the super method
                    throw new IllegalStateException("MongoDBContainer should be started first");
                }
                return String.format(
                        "mongodb://%s:%d/%s",
                        hostName,
                        MONGODB_INTERNAL_PORT,
                        databaseName);
            } else {
                return super.getReplicaSetUrl(databaseName);
            }
        }

        public String getEffectiveHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        public Integer getEffectivePort() {
            return useSharedNetwork ? MONGODB_INTERNAL_PORT : getMappedPort(MONGO_EXPOSED_PORT);
        }

        @Override
        public String getConnectionInfo() {
            return getEffectiveHost() + ":" + getEffectivePort();
        }

        @Override
        public String getContainerId() {
            return super.getContainerId();
        }

        @Override
        public void close() {
            super.close();
        }
    }
}
