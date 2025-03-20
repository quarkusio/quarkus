package io.quarkus.redis.deployment.client;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;
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
import io.quarkus.redis.deployment.client.RedisBuildTimeConfig.DevServiceConfiguration;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { DevServicesConfig.Enabled.class })
public class DevServicesRedisProcessor {
    private static final Logger log = Logger.getLogger(DevServicesRedisProcessor.class);
    private static final String REDIS_IMAGE = "docker.io/redis:7";
    private static final int REDIS_EXPOSED_PORT = 6379;
    private static final String REDIS_SCHEME = "redis://";

    /**
     * Label to add to shared Dev Service for Redis running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-redis";

    private static final ContainerLocator redisContainerLocator = locateContainerWithLabels(REDIS_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    private static final String QUARKUS = "quarkus.";
    private static final String DOT = ".";
    private static volatile List<RunningDevService> devServices;
    private static volatile Map<String, DevServiceConfiguration> capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    @BuildStep
    public List<DevServicesResultBuildItem> startRedisContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            RedisBuildTimeConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        Map<String, DevServiceConfiguration> currentDevServicesConfiguration = new HashMap<>(config.additionalDevServices());
        currentDevServicesConfiguration.put(RedisConfig.DEFAULT_CLIENT_NAME, config.defaultDevService());

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
                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);
                RunningDevService devService = startContainer(dockerStatusBuildItem, composeProjectBuildItem,
                        connectionName,
                        entry.getValue().devservices(),
                        launchMode.getLaunchMode(),
                        useSharedNetwork, devServicesConfig.timeout());
                if (devService == null) {
                    continue;
                }
                newDevServices.add(devService);
                String configKey = getConfigPrefix(connectionName) + RedisConfig.HOSTS_CONFIG_NAME;
                log.infof("The %s redis server is ready to accept connections on %s", connectionName,
                        devService.getConfig().get(configKey));
            }
            if (newDevServices.isEmpty()) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        devServices = newDevServices;

        if (first) {
            first = false;
            Runnable closeTask = () -> {
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

    private RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            String name,
            io.quarkus.redis.deployment.client.DevServicesConfig devServicesConfig, LaunchMode launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(name);

        boolean needToStart = !ConfigUtils.isPropertyNonEmpty(configPrefix + RedisConfig.HOSTS_CONFIG_NAME);
        if (!needToStart) {
            log.debug("Not starting devservices for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " as hosts have been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Please configure quarkus.redis.hosts for "
                    + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " or get a working docker instance");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(devServicesConfig.imageName().orElse(REDIS_IMAGE))
                .asCompatibleSubstituteFor(REDIS_IMAGE);

        Supplier<RunningDevService> defaultRedisServerSupplier = () -> {
            QuarkusPortRedisContainer redisContainer = new QuarkusPortRedisContainer(dockerImageName, devServicesConfig.port(),
                    launchMode == DEVELOPMENT ? devServicesConfig.serviceName() : null,
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork);
            timeout.ifPresent(redisContainer::withStartupTimeout);
            redisContainer.withEnv(devServicesConfig.containerEnv());
            redisContainer.start();
            String redisHost = REDIS_SCHEME + redisContainer.getHost() + ":" + redisContainer.getPort();
            return new RunningDevService(Feature.REDIS_CLIENT.getName(), redisContainer.getContainerId(),
                    redisContainer::close, configPrefix + RedisConfig.HOSTS_CONFIG_NAME, redisHost);
        };

        return redisContainerLocator.locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(devServicesConfig.imageName().orElse("redis")),
                        REDIS_EXPOSED_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    String redisUrl = REDIS_SCHEME + containerAddress.getUrl();
                    return new RunningDevService(Feature.REDIS_CLIENT.getName(), containerAddress.getId(),
                            null, configPrefix + RedisConfig.HOSTS_CONFIG_NAME, redisUrl);
                })
                .orElseGet(defaultRedisServerSupplier);
    }

    private String getConfigPrefix(String name) {
        String configPrefix = QUARKUS + RedisConfig.REDIS_CONFIG_ROOT_NAME + DOT;
        if (!RedisConfig.isDefaultClient(name)) {
            configPrefix = configPrefix + name + DOT;
        }
        return configPrefix;
    }

    private static class QuarkusPortRedisContainer extends GenericContainer<QuarkusPortRedisContainer> {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusPortRedisContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "redis");
        }

        @Override
        protected void configure() {
            super.configure();
            if (useSharedNetwork) {
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
