package io.quarkus.redis.deployment.client;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.redis")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RedisBuildTimeConfig {

    /**
     * The default redis client
     */
    @WithParentName
    RedisClientBuildTimeConfig defaultRedisClient();

    /**
     * Configures additional (named) Redis clients.
     * <p>
     * Each client has a unique name which must be identified to select the right client.
     * For example:
     * <p>
     *
     * <pre>
     * quarkus.redis.client1.hosts = redis://localhost:6379
     * quarkus.redis.client2.hosts = redis://localhost:6380
     * </pre>
     * <p>
     * And then use the {@link io.quarkus.redis.client.RedisClientName} annotation to select the
     * {@link io.vertx.mutiny.redis.client.Redis},
     * {@link io.vertx.redis.client.Redis}, {@link io.vertx.mutiny.redis.client.RedisAPI} and
     * {@link io.vertx.redis.client.RedisAPI} beans.
     * <p>
     *
     * <pre>
     * {
     *     &#64;code
     *     &#64;RedisClientName("client1")
     *     &#64;Inject
     *     RedisAPI redis;
     * }
     * </pre>
     */
    @WithParentName
    @ConfigDocMapKey("redis-client-name")
    Map<String, RedisClientBuildTimeConfig> namedRedisClients();

    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    /**
     * Default Dev services configuration.
     */
    @WithParentName
    DevServiceConfiguration defaultDevService();

    /**
     * Additional dev services configurations
     */
    @WithParentName
    @ConfigDocMapKey("additional-redis-clients")
    Map<String, DevServiceConfiguration> additionalDevServices();

    @ConfigGroup
    public interface DevServiceConfiguration {
        /**
         * Dev Services
         * <p>
         * Dev Services allows Quarkus to automatically start Redis in dev and test mode.
         */
        @ConfigDocSection(generated = true)
        DevServicesConfig devservices();
    }
}
