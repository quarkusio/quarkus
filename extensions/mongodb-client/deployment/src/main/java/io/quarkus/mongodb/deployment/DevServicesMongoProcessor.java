package io.quarkus.mongodb.deployment;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.isDefault;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.mongodb.deployment.devservices.DevServicesMongoResultBuildItem;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

public class DevServicesMongoProcessor {

    private static final Logger log = Logger.getLogger(DevServicesMongoProcessor.class);

    static volatile List<Closeable> closeables;
    static volatile Map<String, CapturedProperties> capturedProperties;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    public DevServicesMongoResultBuildItem startMongo(List<MongoConnectionNameBuildItem> mongoConnections,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<ServiceStartBuildItem> serviceStartBuildItemBuildProducer) {

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
        if (closeables != null) {
            boolean restartRequired = launchMode.getLaunchMode() == LaunchMode.TEST;
            if (!restartRequired) {
                restartRequired = !currentCapturedProperties.equals(capturedProperties);
            }
            if (!restartRequired) {
                return null;
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
        StartResult startResult = startMongo(connectionName, currentCapturedProperties.get(connectionName));
        DevServicesMongoResultBuildItem.Result defaultConnectionResult = null;
        Map<String, DevServicesMongoResultBuildItem.Result> namedConnectionResults = new HashMap<>();
        if (startResult != null) {
            currentCloseables.add(startResult.getCloseable());
            String connectionStringPropertyName = getConfigPrefix(connectionName) + "connection-string";
            String connectionStringPropertyValue = startResult.getUrl();
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(
                    connectionStringPropertyName, connectionStringPropertyValue));
            DevServicesMongoResultBuildItem.Result result = new DevServicesMongoResultBuildItem.Result(
                    Collections.singletonMap(connectionStringPropertyName, connectionStringPropertyValue));
            if (isDefault(connectionName)) {
                defaultConnectionResult = result;
            } else {
                namedConnectionResults.put(connectionName, result);
            }
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
            Thread closeHookThread = new Thread(closeTask, "Mongo container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().removeShutdownHook(closeHookThread);
                }
            });
        }
        closeables = currentCloseables;
        capturedProperties = currentCapturedProperties;

        return new DevServicesMongoResultBuildItem(defaultConnectionResult, namedConnectionResults);
    }

    private StartResult startMongo(String connectionName, CapturedProperties capturedProperties) {
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
            mongoDBContainer = new MongoDBContainer(
                    DockerImageName.parse(capturedProperties.imageName).asCompatibleSubstituteFor("mongo"));
        } else {
            mongoDBContainer = new MongoDBContainer();
        }
        mongoDBContainer.start();
        Optional<String> databaseName = ConfigProvider.getConfig().getOptionalValue(configPrefix + "database", String.class);
        return new StartResult(
                databaseName.map(mongoDBContainer::getReplicaSetUrl).orElse(mongoDBContainer.getReplicaSetUrl()),
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
                devServicesConfig.imageName.orElse(null));
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

        public CapturedProperties(String database, String connectionString, boolean devServicesEnabled, String imageName) {
            this.database = database;
            this.connectionString = connectionString;
            this.devServicesEnabled = devServicesEnabled;
            this.imageName = imageName;
        }

        public String getDatabase() {
            return database;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public boolean isDevServicesEnabled() {
            return devServicesEnabled;
        }

        public String getImageName() {
            return imageName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CapturedProperties that = (CapturedProperties) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(database, that.database)
                    && Objects.equals(connectionString, that.connectionString) && Objects.equals(imageName, that.imageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(database, connectionString, devServicesEnabled, imageName);
        }
    }
}
