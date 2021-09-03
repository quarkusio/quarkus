package io.quarkus.mongodb.deployment;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.isDefault;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
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

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
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

    static volatile List<Closeable> closeables;
    static volatile Map<String, CapturedProperties> capturedProperties;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public void startMongo(List<MongoConnectionNameBuildItem> mongoConnections,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DevServicesConfigResultBuildItem> devServices,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        List<String> connectionNames = new ArrayList<>(mongoConnections.size());
        for (MongoConnectionNameBuildItem mongoConnection : mongoConnections) {
            connectionNames.add(mongoConnection.getName());
        }

        // TODO: handle named connections as well
        if (connectionNames.size() != 1) {
            return;
        }
        if (!isDefault(connectionNames.get(0))) {
            return;
        }

        Map<String, CapturedProperties> currentCapturedProperties = captureProperties(connectionNames,
                mongoClientBuildTimeConfig);

        //figure out if we need to shut down and restart existing databases
        //if not and the DB's have already started we just return
        if (closeables != null) {
            boolean restartRequired = !currentCapturedProperties.equals(capturedProperties);
            if (!restartRequired) {
                return;
            }
            for (Closeable i : closeables) {
                try {
                    i.close();
                } catch (Throwable e) {
                    log.error("Failed to stop database", e);
                }
            }
            closeables = null;
            capturedProperties = null;
        }

        List<Closeable> currentCloseables = new ArrayList<>(mongoConnections.size());

        // TODO: we need to go through each connection
        String connectionName = connectionNames.get(0);
        StartResult startResult;
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Mongo Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            startResult = startMongo(connectionName, currentCapturedProperties.get(connectionName),
                    devServicesSharedNetworkBuildItem.isPresent());
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
        if (startResult != null) {
            currentCloseables.add(startResult.getCloseable());
            String connectionStringPropertyName = getConfigPrefix(connectionName) + "connection-string";
            String connectionStringPropertyValue = startResult.getUrl();
            devServices.produce(
                    new DevServicesConfigResultBuildItem(connectionStringPropertyName, connectionStringPropertyValue));
        }

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (closeables != null) {
                        for (Closeable i : closeables) {
                            try {
                                i.close();
                            } catch (Throwable t) {
                                log.error("Failed to stop database", t);
                            }
                        }
                    }
                    first = true;
                    closeables = null;
                    capturedProperties = null;
                }
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        }
        closeables = currentCloseables;
        capturedProperties = currentCapturedProperties;

    }

    private StartResult startMongo(String connectionName, CapturedProperties capturedProperties, boolean useSharedNetwork) {
        if (!capturedProperties.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " as it has been disabled in the config");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Please configure datasource URL for "
                    + (isDefault(connectionName) ? "default datasource" : connectionName)
                    + " or get a working docker instance");
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

        MongoDBContainer mongoDBContainer;
        if (capturedProperties.imageName != null) {
            mongoDBContainer = new QuarkusMongoDBContainer(
                    DockerImageName.parse(capturedProperties.imageName).asCompatibleSubstituteFor("mongo"),
                    capturedProperties.fixedExposedPort, useSharedNetwork);
        } else {
            mongoDBContainer = new QuarkusMongoDBContainer(capturedProperties.fixedExposedPort, useSharedNetwork);
        }
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
        return new StartResult(
                effectiveURL,
                new Closeable() {
                    @Override
                    public void close() {
                        mongoDBContainer.close();
                    }
                });
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
                devServicesConfig.imageName.orElse(null), devServicesConfig.port.orElse(null), devServicesConfig.properties);
    }

    private static class StartResult {
        private final String url;
        private final Closeable closeable;

        public StartResult(String url, Closeable closeable) {
            this.url = url;
            this.closeable = closeable;
        }

        public String getUrl() {
            return url;
        }

        public Closeable getCloseable() {
            return closeable;
        }
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
