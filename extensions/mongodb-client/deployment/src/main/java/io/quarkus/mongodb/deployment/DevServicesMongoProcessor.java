package io.quarkus.mongodb.deployment;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.isDefault;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.configuration.ConfigUtils;

public class DevServicesMongoProcessor {

    private static final Logger log = Logger.getLogger(DevServicesMongoProcessor.class);

    static volatile List<RunningDevService> devServices;
    static volatile Map<String, CapturedProperties> capturedProperties;
    static volatile boolean first = true;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public List<DevServicesResultBuildItem> startMongo(List<MongoConnectionNameBuildItem> mongoConnections,
            DockerStatusBuildItem dockerStatusBuildItem,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {

        List<String> connectionNames = new ArrayList<>(mongoConnections.size());
        for (MongoConnectionNameBuildItem mongoConnection : mongoConnections) {
            connectionNames.add(mongoConnection.getName());
        }

        // TODO: handle named connections as well
        if (connectionNames.size() != 1) {
            return null;
        }
        if (!isDefault(connectionNames.get(0))) {
            return null;
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

        // TODO: we need to go through each connection
        String connectionName = connectionNames.get(0);
        RunningDevService devService;
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Mongo Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            devService = startMongo(dockerStatusBuildItem, connectionName, currentCapturedProperties.get(connectionName),
                    !devServicesSharedNetworkBuildItem.isEmpty(), globalDevServicesConfig.timeout);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
        if (devService != null) {
            newDevServices.add(devService);
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

    private RunningDevService startMongo(DockerStatusBuildItem dockerStatusBuildItem, String connectionName,
            CapturedProperties capturedProperties, boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!capturedProperties.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(connectionName);

        // TODO: do we need to check the hosts as well?
        boolean needToStart = !ConfigUtils.isPropertyPresent(configPrefix + "connection-string");
        if (!needToStart) {
            // a connection string has been provided
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " as a connection string has been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Please configure datasource URL for "
                    + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " or get a working docker instance");
            return null;
        }
        MongoDBContainer mongoDBContainer;
        if (capturedProperties.imageName != null) {
            mongoDBContainer = new QuarkusMongoDBContainer(
                    DockerImageName.parse(capturedProperties.imageName).asCompatibleSubstituteFor("mongo"),
                    capturedProperties.fixedExposedPort, useSharedNetwork);
        } else {
            mongoDBContainer = new QuarkusMongoDBContainer(capturedProperties.fixedExposedPort, useSharedNetwork);
        }
        timeout.ifPresent(mongoDBContainer::withStartupTimeout);
        mongoDBContainer.start();
        Optional<String> databaseName = ConfigProvider.getConfig().getOptionalValue(configPrefix + "database", String.class);
        String effectiveURL = databaseName.map(mongoDBContainer::getReplicaSetUrl).orElse(mongoDBContainer.getReplicaSetUrl());
        if ((capturedProperties.connectionProperties != null) && !capturedProperties.connectionProperties.isEmpty()) {
            effectiveURL = effectiveURL + "?"
                    + URLEncodedUtils.format(
                            capturedProperties.connectionProperties.entrySet().stream()
                                    .map(e -> new BasicNameValuePair(e.getKey(), e.getValue())).collect(Collectors.toList()),
                            StandardCharsets.UTF_8);
        }
        return new RunningDevService(Feature.MONGODB_CLIENT.getName(), mongoDBContainer.getContainerId(),
                mongoDBContainer::close, getConfigPrefix(connectionName) + "connection-string", effectiveURL);
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
        DevServicesBuildTimeConfig devServicesConfig = mongoClientBuildTimeConfig.devservices;
        boolean devServicesEnabled = devServicesConfig.enabled.orElse(true);
        return new CapturedProperties(databaseName, connectionString, devServicesEnabled,
                devServicesConfig.imageName.orElseGet(() -> ConfigureUtil.getDefaultImageNameFor("mongo")),
                devServicesConfig.port.orElse(null), devServicesConfig.properties);
    }

    private static final class CapturedProperties {
        private final String database;
        private final String connectionString;
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final Map<String, String> connectionProperties;

        public CapturedProperties(String database, String connectionString, boolean devServicesEnabled, String imageName,
                Integer fixedExposedPort, Map<String, String> connectionProperties) {
            this.database = database;
            this.connectionString = connectionString;
            this.devServicesEnabled = devServicesEnabled;
            this.imageName = imageName;
            this.fixedExposedPort = fixedExposedPort;
            this.connectionProperties = connectionProperties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CapturedProperties that = (CapturedProperties) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(database, that.database)
                    && Objects.equals(connectionString, that.connectionString) && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && Objects.equals(connectionProperties, that.connectionProperties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(database, connectionString, devServicesEnabled, imageName, fixedExposedPort,
                    connectionProperties);
        }
    }

    private static final class QuarkusMongoDBContainer extends MongoDBContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        private static final int MONGODB_INTERNAL_PORT = 27017;

        @SuppressWarnings("deprecation")
        private QuarkusMongoDBContainer(Integer fixedExposedPort, boolean useSharedNetwork) {
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        private QuarkusMongoDBContainer(DockerImageName dockerImageName, Integer fixedExposedPort, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "mongo");
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
    }
}
