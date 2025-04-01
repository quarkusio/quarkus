package io.quarkus.redis.deployment.client;

import static io.quarkus.devservices.common.ContainerLocator.MANAGED_DEV_SERVICE_LABEL;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;
import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

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
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunnableDevService;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
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
    private static final String LAUNCH_MODE_LABEL = "quarkus-launch-mode";

    private static final ContainerLocator redisContainerLocator = locateContainerWithLabels(REDIS_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    private static final String QUARKUS = "quarkus.";
    private static final String DOT = ".";

    @BuildStep
    public List<DevServicesResultBuildItem> startRedisContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            RedisBuildTimeConfig config,
            DevServicesRegistryBuildItem tracker,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        Map<String, DevServiceConfiguration> currentDevServicesConfiguration = new HashMap<>(config.additionalDevServices());
        currentDevServicesConfiguration.put(RedisConfig.DEFAULT_CLIENT_NAME, config.defaultDevService());

        // We cannot assume an augmentation order, so we cannot just check and reuse previous dev services.
        // We *could* get the services from the tracker, and short circuit some work. But that short circuit has some risk.
        // If the matching RunningDevService was in a different classloader, we'd get a ClassCastException.

        List<RunningDevService> newDevServices = new ArrayList<>();

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Redis Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            for (Entry<String, DevServiceConfiguration> entry : currentDevServicesConfiguration.entrySet()) {
                String connectionName = entry.getKey();
                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);

                // We always get a RunnableDevServices object back, but multiple of these objects might map to the same underlying container, *if* the container locator finds services
                RunningDevService devService = createContainer(dockerStatusBuildItem, composeProjectBuildItem,
                        connectionName,
                        entry.getValue().devservices(),
                        launchMode.getLaunchMode(),
                        useSharedNetwork, devServicesConfig.timeout(), tracker);
                if (devService == null) {
                    continue;
                }

                newDevServices.add(devService);
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

        return newDevServices.stream().map(RunningDevService::toBuildItem).collect(Collectors.toList());
    }

    private RunningDevService createContainer(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            String name,
            io.quarkus.redis.deployment.client.DevServicesConfig devServicesConfig, LaunchMode launchMode,
            boolean useSharedNetwork, Optional<Duration> timeout, DevServicesRegistryBuildItem tracker) {

        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " as it has been disabled in the config");
            return null;
        }

        String configPrefix = getConfigPrefix(name);

        boolean needToStart = !ConfigUtils.isPropertyNonEmpty(configPrefix + RedisConfig.HOSTS_CONFIG_NAME);
        if (!needToStart) {
            log.debug("Not starting dev services for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
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
            OptionalInt fixedExposedPort = devServicesConfig.port();
            String serviceName = launchMode == DEVELOPMENT ? devServicesConfig.serviceName() : null;
            String defaultNetworkId = composeProjectBuildItem.getDefaultNetworkId();
            QuarkusPortRedisContainer redisContainer = new QuarkusPortRedisContainer(dockerImageName, fixedExposedPort,
                    serviceName,
                    defaultNetworkId,
                    useSharedNetwork, launchMode);
            timeout.ifPresent(redisContainer::withStartupTimeout);
            redisContainer.withEnv(devServicesConfig.containerEnv());

            Map<String, Supplier<String>> dynamicConfig = new HashMap<>();
            dynamicConfig.put(configPrefix + RedisConfig.HOSTS_CONFIG_NAME,
                    () -> REDIS_SCHEME + redisContainer.getHost() + ":" + redisContainer.getPort());

            return new RunnableDevService(Feature.REDIS_CLIENT.getName(), name, redisContainer,
                    dynamicConfig, devServicesConfig, tracker);
        };

        // The ideal re-use precedence order is the following:
        // 1. Re-use existing dev service/container if one with compatible config exists (only knowable post-augmentation, or on the second run of a continuous testing session)
        // 2. Use the container locator to find an external service (where applicable) (knowable at augmentation)
        // 3. Create a new container
        // This swaps 1 and 2, but that's actually ok. If an external service exists and is valid for this configuration,
        // any matching service would be using it, so option 1 (an existing internal container) can't happen.
        // If there's no external service, then the order is 1 and then 3, which is what we want.
        // Because of how the labelling works, dev services we create will not be detected by the locator.
        // The check for running services happens in RunnableDevService.start(), because it has to happen at runtime, not during augmentation.
        // We cannot assume the order of container creation in augmentation would be the same as the runtime order.

        // The container locator might find services from other tests, which would not be ok because they'd have the wrong config
        // We can be fairly confident this isn't happening because of the tests showing config is honoured, but if we wanted to be extra sure we'd put on a special 'not external' label and filter for that, too
        return redisContainerLocator.locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(devServicesConfig.imageName().orElse("redis")),
                        REDIS_EXPOSED_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    String redisUrl = REDIS_SCHEME + containerAddress.getUrl();
                    // If there's a pre-existing container, it's already running, so create a running container, not a runnable one
                    return new RunningDevService(Feature.REDIS_CLIENT.getName(),
                            containerAddress.getId(),
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

    private static class QuarkusPortRedisContainer extends GenericContainer<QuarkusPortRedisContainer> implements Startable {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusPortRedisContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, String serviceName,
                String defaultNetworkId, boolean useSharedNetwork, LaunchMode launchMode) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            if (serviceName != null) {
                withLabel(DEV_SERVICE_LABEL, serviceName);
                withLabel(QUARKUS_DEV_SERVICE, serviceName);
            }
            withLabel(MANAGED_DEV_SERVICE_LABEL, RunningDevServicesRegistry.APPLICATION_UUID);
            withLabel(LAUNCH_MODE_LABEL, launchMode.toString());

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

        public void close() {
            super.close();
        }

        @Override
        public String getConnectionInfo() {
            return getHost() + ":" + getPort();
        }
    }
}
