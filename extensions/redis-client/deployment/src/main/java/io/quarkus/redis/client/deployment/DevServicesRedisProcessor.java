package io.quarkus.redis.client.deployment;

import static io.quarkus.redis.client.runtime.RedisClientUtil.isDefault;
import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDockerWorking.IsDockerRunningSilent;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.redis.client.deployment.RedisBuildTimeConfig.DevServiceConfiguration;
import io.quarkus.redis.client.runtime.RedisClientUtil;
import io.quarkus.redis.client.runtime.RedisConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

public class DevServicesRedisProcessor {
    private static final Logger log = Logger.getLogger(DevServicesRedisProcessor.class);
    private static final String REDIS_6_ALPINE = "docker.io/redis:6-alpine";
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
    private static volatile List<RunningDevService> devServices;
    private static volatile Map<String, DevServiceConfiguration> capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private static volatile Boolean dockerRunning = null;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
    public List<DevServicesResultBuildItem> startRedisContainers(LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            RedisBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        Map<String, DevServiceConfiguration> currentDevServicesConfiguration = new HashMap<>(config.additionalDevServices);
        currentDevServicesConfiguration.put(RedisClientUtil.DEFAULT_CLIENT, config.defaultDevService);

        // figure out if we need to shut down and restart existing redis containers
        // if not and the redis containers have already started we just return
        if (devServices != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
            }
            for (Closeable closeable : devServices) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    log.error("Failed to stop redis container", e);
                }
            }
            devServices = null;
            capturedDevServicesConfiguration = null;
        }

        capturedDevServicesConfiguration = currentDevServicesConfiguration;
        List<RunningDevService> newDevServices = new ArrayList<>();

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Redis Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            for (Entry<String, DevServiceConfiguration> entry : currentDevServicesConfiguration.entrySet()) {
                String connectionName = entry.getKey();
                RunningDevService devService = startContainer(connectionName, entry.getValue().devservices,
                        launchMode.getLaunchMode(),
                        !devServicesSharedNetworkBuildItem.isEmpty(), devServicesConfig.timeout);
                if (devService == null) {
                    continue;
                }
                newDevServices.add(devService);
                String configKey = getConfigPrefix(connectionName) + RedisConfig.HOSTS_CONFIG_NAME;
                log.infof("The %s redis server is ready to accept connections on %s", connectionName,
                        devService.getConfig().get(configKey));
            }
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        devServices = newDevServices;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                dockerRunning = null;
                if (devServices != null) {
                    for (Closeable closeable : devServices) {
                        try {
                            closeable.close();
                        } catch (Throwable t) {
                            log.error("Failed to stop database", t);
                        }
                    }
                }
                first = true;
                devServices = null;
                capturedDevServicesConfiguration = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        return devServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
    }

    private RunningDevService startContainer(String connectionName, DevServicesConfig devServicesConfig, LaunchMode launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
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

        if (dockerRunning == null) {
            dockerRunning = new IsDockerRunningSilent().getAsBoolean();
        }

        if (!dockerRunning) {
            log.warn("Please configure quarkus.redis.hosts for "
                    + (isDefault(connectionName) ? "default redis client" : connectionName)
                    + " or get a working docker instance");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName.orElse(REDIS_6_ALPINE))
                .asCompatibleSubstituteFor(REDIS_6_ALPINE);

        Supplier<RunningDevService> defaultRedisServerSupplier = () -> {
            QuarkusPortRedisContainer redisContainer = new QuarkusPortRedisContainer(dockerImageName, devServicesConfig.port,
                    launchMode == DEVELOPMENT ? devServicesConfig.serviceName : null, useSharedNetwork);
            timeout.ifPresent(redisContainer::withStartupTimeout);
            redisContainer.start();
            String redisHost = REDIS_SCHEME + redisContainer.getHost() + ":" + redisContainer.getPort();
            return new RunningDevService(Feature.REDIS_CLIENT.getName(), redisContainer.getContainerId(),
                    redisContainer::close, configPrefix + RedisConfig.HOSTS_CONFIG_NAME, redisHost);
        };

        return redisContainerLocator.locateContainer(devServicesConfig.serviceName, devServicesConfig.shared, launchMode)
                .map(containerAddress -> {
                    String redisUrl = REDIS_SCHEME + containerAddress.getUrl();
                    return new RunningDevService(Feature.REDIS_CLIENT.getName(), containerAddress.getId(),
                            null, configPrefix + RedisConfig.HOSTS_CONFIG_NAME, redisUrl);
                })
                .orElseGet(defaultRedisServerSupplier);
    }

    private String getConfigPrefix(String connectionName) {
        String configPrefix = QUARKUS + RedisConfig.REDIS_CONFIG_ROOT_NAME + DOT;
        if (!isDefault(connectionName)) {
            configPrefix = configPrefix + connectionName + DOT;
        }
        return configPrefix;
    }

    private static class QuarkusPortRedisContainer extends GenericContainer<QuarkusPortRedisContainer> {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private String hostName = null;

        public QuarkusPortRedisContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "redis");
                return;
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), REDIS_EXPOSED_PORT);
            } else {
                addExposedPort(REDIS_EXPOSED_PORT);
            }
        }

        public int getPort() {
            if (useSharedNetwork) {
                return REDIS_EXPOSED_PORT;
            }

            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return super.getFirstMappedPort();
        }

        @Override
        public String getHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }
    }
}
