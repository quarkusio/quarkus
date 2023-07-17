package io.quarkus.redis.client.deployment;

import java.util.Map;
import java.util.Objects;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class RedisBuildTimeConfig {

    /**
     * The default redis client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public RedisClientBuildTimeConfig defaultRedisClient;

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
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("redis-client-name")
    public Map<String, RedisClientBuildTimeConfig> namedRedisClients;

    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Default Dev services configuration.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DevServiceConfiguration defaultDevService;

    /**
     * Additional dev services configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("additional-redis-clients")
    public Map<String, DevServiceConfiguration> additionalDevServices;

    @ConfigGroup
    public static class DevServiceConfiguration {
        /**
         * Configuration for DevServices
         * <p>
         * DevServices allows Quarkus to automatically start Redis in dev and test mode.
         */
        @ConfigItem
        public DevServicesConfig devservices;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            DevServiceConfiguration that = (DevServiceConfiguration) o;
            return Objects.equals(devservices, that.devservices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devservices);
        }
    }
}
