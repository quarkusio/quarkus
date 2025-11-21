package io.quarkus.redis.deployment.client;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

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
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesRedisProcessor {
    private static final Logger log = Logger.getLogger(DevServicesRedisProcessor.class);

    private static final int REDIS_EXPOSED_PORT = 6379;
    private static final String REDIS_SCHEME = "redis://";

    /**
     * Label to add to shared Dev Service for Redis running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-redis";

    private static final ContainerLocator redisContainerLocator = locateContainerWithLabels(REDIS_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    @BuildStep
    public void startRedisContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            RedisBuildTimeConfig config,
            BuildProducer<DevServicesResultBuildItem> devServicesResult,
            DevServicesConfig devServicesConfig) {

        Set<String> names = new HashSet<>(config.clients().keySet());
        names.add(RedisConfig.DEFAULT_CLIENT_NAME);

        try {
            for (String name : names) {
                boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                        devServicesSharedNetworkBuildItem);

                io.quarkus.redis.deployment.client.DevServicesConfig redisConfig = config.clients().get(name).devservices();
                if (redisDevServicesEnabled(dockerStatusBuildItem, name, redisConfig)) {
                    // If the dev services are disabled, we don't need to do anything
                    continue;
                }

                DevServicesResultBuildItem discovered = discoverRunningService(composeProjectBuildItem, name,
                        redisConfig, launchMode.getLaunchMode(), useSharedNetwork);
                if (discovered != null) {
                    devServicesResult.produce(discovered);
                } else {
                    devServicesResult
                            .produce(DevServicesResultBuildItem.owned().feature(Feature.REDIS_CLIENT)
                                    .serviceName(name)
                                    .serviceConfig(redisConfig)
                                    .startable(() -> new QuarkusPortRedisContainer(
                                            DockerImageName
                                                    .parse(redisConfig.imageName()
                                                            .orElseGet(() -> getDefaultImageNameFor("redis")))
                                                    .asCompatibleSubstituteFor("redis"),
                                            redisConfig.port(),
                                            composeProjectBuildItem.getDefaultNetworkId(),
                                            useSharedNetwork)
                                            .withEnv(redisConfig.containerEnv())
                                            // Dev Service discovery works using a global dev service label applied in DevServicesCustomizerBuildItem
                                            // for backwards compatibility we still add the custom label
                                            .withSharedServiceLabel(launchMode.getLaunchMode(), redisConfig.serviceName()))
                                    .configProvider(
                                            Map.of(getPropertyName(name, HOSTS), s -> REDIS_SCHEME + s.getConnectionInfo()))
                                    .build());
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * The ideal re-use precedence order is the following:
     * 1. Re-use existing dev service/container if one with compatible config exists (only knowable post-augmentation, or on the
     * second run of a continuous testing session)
     * 2. Use the container locator to find an external service (where applicable) (knowable at augmentation)
     * 3. Create a new container
     * This swaps 1 and 2, but that's actually ok. If an external service exists and is valid for this configuration,
     * any matching service would be using it, so option 1 (an existing internal container) can't happen.
     * If there's no external service, then the order is 1 and then 3, which is what we want.
     * Because of how the labelling works, dev services we create will not be detected by the locator.
     * The check for running services happens in RunnableDevService.start(), because it has to happen at runtime, not during
     * augmentation.
     * We cannot assume the order of container creation in augmentation would be the same as the runtime order.
     *
     * The container locator might find services from other tests, which would not be ok because they'd have the wrong config
     * We can be fairly confident this isn't happening because of the tests showing config is honoured, but if we wanted to be
     * extra sure we'd put on a special 'not external' label and filter for that, too
     */
    private DevServicesResultBuildItem discoverRunningService(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            String name,
            io.quarkus.redis.deployment.client.DevServicesConfig devServicesConfig,
            LaunchMode launchMode,
            boolean useSharedNetwork) {
        return redisContainerLocator.locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(devServicesConfig.imageName().orElseGet(() -> getDefaultImageNameFor("redis"))),
                        REDIS_EXPOSED_PORT, launchMode, useSharedNetwork))
                .map(containerAddress -> {
                    String redisUrl = REDIS_SCHEME + containerAddress.getUrl();
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.REDIS_CLIENT)
                            .containerId(containerAddress.getId())
                            .config(Map.of(RedisConfig.getPropertyName(name, HOSTS), redisUrl))
                            .build();
                }).orElse(null);
    }

    private static boolean redisDevServicesEnabled(DockerStatusBuildItem dockerStatusBuildItem, String name,
            io.quarkus.redis.deployment.client.DevServicesConfig devServicesConfig) {
        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            log.debug("Not starting devservices for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " as it has been disabled in the config");
            return true;
        }

        // TODO - We shouldn't query runtime config during deployment
        boolean needToStart = !ConfigUtils.isPropertyNonEmpty(RedisConfig.getPropertyName(name, HOSTS));
        if (!needToStart) {
            log.debug("Not starting dev services for " + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " as hosts have been provided");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Please configure quarkus.redis.hosts for "
                    + (RedisConfig.isDefaultClient(name) ? "default redis client" : name)
                    + " or get a working docker instance");
            return true;
        }
        return false;
    }

    private static class QuarkusPortRedisContainer extends GenericContainer<QuarkusPortRedisContainer> implements Startable {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;

        private final String hostName;

        public QuarkusPortRedisContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;

            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "redis");
        }

        public QuarkusPortRedisContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
            return configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
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
