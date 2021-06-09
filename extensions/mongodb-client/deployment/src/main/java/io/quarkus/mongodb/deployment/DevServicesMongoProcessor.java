package io.quarkus.mongodb.deployment;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.isDefault;

import java.io.Closeable;
import java.util.ArrayList;
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
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
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
    public void startMongo(List<MongoConnectionNameBuildItem> mongoConnections,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<ServiceStartBuildItem> serviceStartBuildItemBuildProducer,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServices) {

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
            boolean restartRequired = launchMode.getLaunchMode() == LaunchMode.TEST;
            if (!restartRequired) {
                restartRequired = !currentCapturedProperties.equals(capturedProperties);
            }
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
        StartResult startResult = startMongo(connectionName, currentCapturedProperties.get(connectionName));
        if (startResult != null) {
            currentCloseables.add(startResult.getCloseable());
            String connectionStringPropertyName = getConfigPrefix(connectionName) + "connection-string";
            String connectionStringPropertyValue = startResult.getUrl();
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(
                    connectionStringPropertyName, connectionStringPropertyValue));
            devServices.produce(
                    new DevServicesNativeConfigResultBuildItem(connectionStringPropertyName, connectionStringPropertyValue));
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
            mongoDBContainer = new FixedExposedPortMongoDBContainer(
                    DockerImageName.parse(capturedProperties.imageName).asCompatibleSubstituteFor("mongo"),
                    capturedProperties.fixedExposedPort);
        } else {
            mongoDBContainer = new FixedExposedPortMongoDBContainer(capturedProperties.fixedExposedPort);
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
                devServicesConfig.imageName.orElse(null), devServicesConfig.port.orElse(null));
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

        public CapturedProperties(String database, String connectionString, boolean devServicesEnabled, String imageName,
                Integer fixedExposedPort) {
            this.database = database;
            this.connectionString = connectionString;
            this.devServicesEnabled = devServicesEnabled;
            this.imageName = imageName;
            this.fixedExposedPort = fixedExposedPort;
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
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(database, connectionString, devServicesEnabled, imageName, fixedExposedPort);
        }
    }

    private static final class FixedExposedPortMongoDBContainer extends MongoDBContainer {

        private final Integer fixedExposedPort;

        @SuppressWarnings("deprecation")
        private FixedExposedPortMongoDBContainer(Integer fixedExposedPort) {
            this.fixedExposedPort = fixedExposedPort;
        }

        private FixedExposedPortMongoDBContainer(DockerImageName dockerImageName, Integer fixedExposedPort) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
        }

        @Override
        protected void configure() {
            super.configure();
            if (fixedExposedPort != null) {
                addFixedExposedPort(fixedExposedPort, 27017);
            }
        }
    }
}
