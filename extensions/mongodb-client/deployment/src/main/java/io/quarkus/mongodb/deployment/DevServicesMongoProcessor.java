package io.quarkus.mongodb.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.isDefault;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.message.BasicNameValuePair;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URLEncodedUtils;

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
import io.quarkus.devservices.common.Labels;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class DevServicesMongoProcessor {

    private static final Logger log = Logger.getLogger(DevServicesMongoProcessor.class);

    static volatile List<RunningDevService> devServices;
    static volatile Map<String, CapturedProperties> capturedProperties;
    static volatile boolean first = true;

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
    public List<DevServicesResultBuildItem> startMongo(List<MongoConnectionNameBuildItem> mongoConnections,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        List<String> connectionNames = new ArrayList<>(mongoConnections.size());
        for (MongoConnectionNameBuildItem mongoConnection : mongoConnections) {
            connectionNames.add(mongoConnection.getName());
        }

        Map<String, CapturedProperties> currentCapturedProperties = captureProperties(connectionNames,
                mongoClientBuildTimeConfig);

        //figure out if we need to shut down and restart existing databases
        //if not and the DB's have already started we just return
        if (devServices != null) {
            boolean restartRequired = !currentCapturedProperties.equals(capturedProperties);
            if (!restartRequired) {
                return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
            }
            for (Closeable i : devServices) {
                try {
                    i.close();
                } catch (Throwable e) {
                    log.error("Failed to stop database", e);
                }
            }
            devServices = null;
            capturedProperties = null;
        }

        List<RunningDevService> newDevServices = new ArrayList<>(mongoConnections.size());

        for (String connectionName : connectionNames) {
            RunningDevService devService;
            StartupLogCompressor compressor = new StartupLogCompressor(
                    (launchMode.isTest() ? "(test) " : "") + "Mongo Dev Services Starting:", consoleInstalledBuildItem,
                    loggingSetupBuildItem);
            try {
                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);
                devService = startMongo(dockerStatusBuildItem, composeProjectBuildItem, connectionName,
                        currentCapturedProperties.get(connectionName),
                        useSharedNetwork, devServicesConfig.timeout(), launchMode.getLaunchMode(),
                        mongoClientBuildTimeConfig.devservices().serviceName());
                if (devService == null) {
                    compressor.closeAndDumpCaptured();
                } else {
                    compressor.close();
                    newDevServices.add(devService);
                }
            } catch (Throwable t) {
                compressor.closeAndDumpCaptured();
                throw new RuntimeException(t);
            }
        }

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (devServices != null) {
                        for (Closeable i : devServices) {
                            try {
                                i.close();
                            } catch (Throwable t) {
                                log.error("Failed to stop database", t);
                            }
                        }
                    }
                    first = true;
                    devServices = null;
                    capturedProperties = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        devServices = newDevServices;
        capturedProperties = currentCapturedProperties;
        return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
    }

    private RunningDevService startMongo(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            String connectionName, CapturedProperties capturedProperties,
            boolean useSharedNetwork, Optional<Duration> timeout,
            LaunchMode launchMode, String serviceName) {
        if (!capturedProperties.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(connectionName);

        boolean needToStart = !ConfigUtils.isPropertyNonEmpty(configPrefix + "connection-string")
                && !ConfigUtils.isPropertyNonEmpty(configPrefix + "hosts");
        if (!needToStart) {
            // a connection string has been provided
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " as a connection string and/or server addresses have been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Please configure datasource URL for "
                    + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " or get a working docker instance");
            return null;
        }

        Supplier<RunningDevService> defaultMongoServerSupplier = () -> {
            QuarkusMongoDBContainer mongoDBContainer;
            if (capturedProperties.imageName != null) {
                mongoDBContainer = new QuarkusMongoDBContainer(
                        DockerImageName.parse(capturedProperties.imageName).asCompatibleSubstituteFor("mongo"),
                        capturedProperties.fixedExposedPort,
                        composeProjectBuildItem.getDefaultNetworkId(),
                        useSharedNetwork, launchMode, serviceName);
            } else {
                mongoDBContainer = new QuarkusMongoDBContainer(capturedProperties.fixedExposedPort,
                        composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, launchMode, serviceName);
            }
            timeout.ifPresent(mongoDBContainer::withStartupTimeout);
            mongoDBContainer.withEnv(capturedProperties.containerEnv);
            mongoDBContainer.start();

            final String effectiveUrl = getEffectiveUrl(configPrefix, mongoDBContainer.getEffectiveHost(),
                    mongoDBContainer.getEffectivePort(), capturedProperties);
            return new RunningDevService(Feature.MONGODB_CLIENT.getName(), mongoDBContainer.getContainerId(),
                    mongoDBContainer::close, getConfigPrefix(connectionName) + "connection-string", effectiveUrl);
        };

        return MONGO_CONTAINER_LOCATOR
                .locateContainer(capturedProperties.serviceName(), capturedProperties.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(capturedProperties.imageName, "mongo"), MONGO_EXPOSED_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    final String effectiveUrl = getEffectiveUrl(configPrefix, containerAddress.getHost(),
                            containerAddress.getPort(), capturedProperties);

                    return new RunningDevService(Feature.MONGODB_CLIENT.getName(), containerAddress.getId(),
                            null, getConfigPrefix(connectionName) + "connection-string", effectiveUrl);
                })
                .orElseGet(defaultMongoServerSupplier);
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
        String configPrefix = "quarkus." + MongodbConfig.CONFIG_NAME + ".";
        if (!isDefault(connectionName)) {
            configPrefix = configPrefix + connectionName + ".";
        }
        return configPrefix;
    }

    private Map<String, CapturedProperties> captureProperties(List<String> connectionNames,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig) {
        Map<String, CapturedProperties> result = new HashMap<>();
        for (String connectionName : connectionNames) {
            result.put(connectionName, captureProperties(connectionName, mongoClientBuildTimeConfig));
        }
        return result;
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

    private static final class QuarkusMongoDBContainer extends MongoDBContainer {

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
    }
}
