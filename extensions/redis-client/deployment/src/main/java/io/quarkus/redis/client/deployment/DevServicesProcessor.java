package io.quarkus.redis.client.deployment;

import static io.quarkus.redis.client.runtime.RedisClientUtil.isDefault;
import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking.IsDockerRunningSilent;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.redis.client.deployment.RedisBuildTimeConfig.DevServiceConfiguration;
import io.quarkus.redis.client.runtime.RedisClientUtil;
import io.quarkus.redis.client.runtime.RedisConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

public class DevServicesProcessor {
    private static final Logger log = Logger.getLogger(DevServicesProcessor.class);
    private static final String REDIS_6_ALPINE = "redis:6-alpine";
    private static final int REDIS_EXPOSED_PORT = 6379;
    private static final String REDIS_SCHEME = "redis://";

    /**
     * Label to add to shared Dev Service for Redis running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-redis";

    private static final ContainerLocator redisContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, REDIS_EXPOSED_PORT);

    private static final String QUARKUS = "quarkus.";
    private static final String DOT = ".";
    private static volatile List<Closeable> closeables;
    private static volatile Map<String, DevServiceConfiguration> capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @Produce(ServiceStartBuildItem.class)
    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = IsDockerRunningSilent.class)
    public void startRedisContainers(LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,

            BuildProducer<DevServicesNativeConfigResultBuildItem> devConfigProducer, RedisBuildTimeConfig config) {

        Map<String, DevServiceConfiguration> currentDevServicesConfiguration = new HashMap<>(config.additionalDevServices);
        currentDevServicesConfiguration.put(RedisClientUtil.DEFAULT_CLIENT, config.defaultDevService);

        // figure out if we need to shut down and restart existing redis containers
        // if not and the redis containers have already started we just return
        if (closeables != null) {
            boolean restartRequired = launchMode.getLaunchMode() == LaunchMode.TEST;
            if (!restartRequired) {
                restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            }
            if (!restartRequired) {
                return;
            }
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop redis container", e);
                }
            }
            closeables = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;
        List<Closeable> currentCloseables = new ArrayList<>();
        for (Entry<String, DevServiceConfiguration> entry : currentDevServicesConfiguration.entrySet()) {
            String connectionName = entry.getKey();
            StartResult startResult = startContainer(connectionName, entry.getValue().devservices, launchMode.getLaunchMode());
            if (startResult == null) {
                continue;
            }
            currentCloseables.add(startResult.closeable);
            String configKey = getConfigPrefix(connectionName) + RedisConfig.HOSTS_CONFIG_NAME;
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(configKey, startResult.url));
            devConfigProducer.produce(new DevServicesNativeConfigResultBuildItem(configKey, startResult.url));
        }

        closeables = currentCloseables;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (closeables != null) {
                    for (Closeable closeable : closeables) {
                        try {
                            closeable.close();
                        } catch (Throwable t) {
                            log.error("Failed to stop database", t);
                        }
                    }
                }
                first = true;
                closeables = null;
                capturedDevServicesConfiguration = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Redis container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(() -> Runtime.getRuntime().removeShutdownHook(closeHookThread));
        }
    }

    private StartResult startContainer(String connectionName, DevServicesConfig devServicesConfig, LaunchMode launchMode) {
        if (!devServicesConfig.enabled) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default redis client" : connectionName)
                    + " as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(connectionName);

        boolean needToStart = !ConfigUtils.isPropertyPresent(configPrefix + RedisConfig.HOSTS_CONFIG_NAME);
        if (!needToStart) {
            log.debug("Not starting devservices for " + (isDefault(connectionName) ? "default redis client" : connectionName)
                    + " as hosts have been provided");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName.orElse(REDIS_6_ALPINE))
                .asCompatibleSubstituteFor(REDIS_6_ALPINE);

        if (devServicesConfig.shared && launchMode == DEVELOPMENT) {
            final String url = redisContainerLocator.locateContainer(devServicesConfig.serviceName);
            return new StartResult(url, null);
        }

        FixedPortRedisContainer redisContainer = new FixedPortRedisContainer(dockerImageName, devServicesConfig.port, launchMode == DEVELOPMENT ? devServicesConfig.serviceName : null);
        redisContainer.start();
        String redisHost = REDIS_SCHEME + redisContainer.getHost() + ":" + redisContainer.getPort();
        return new StartResult(redisHost,
                redisContainer::close);
    }

    private String getConfigPrefix(String connectionName) {
        String configPrefix = QUARKUS + RedisConfig.REDIS_CONFIG_ROOT_NAME + DOT;
        if (!isDefault(connectionName)) {
            configPrefix = configPrefix + connectionName + DOT;
        }
        return configPrefix;
    }

    private static class StartResult {
        private final String url;
        private final Closeable closeable;

        public StartResult(String url, Closeable closeable) {
            this.url = url;
            this.closeable = closeable;
        }
    }

    private static class FixedPortRedisContainer extends GenericContainer<FixedPortRedisContainer> {
        OptionalInt fixedExposedPort;

        public FixedPortRedisContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), REDIS_EXPOSED_PORT);
            } else {
                addExposedPort(REDIS_EXPOSED_PORT);
            }
        }

        public int getPort() {
            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return super.getFirstMappedPort();
        }
    }
}
