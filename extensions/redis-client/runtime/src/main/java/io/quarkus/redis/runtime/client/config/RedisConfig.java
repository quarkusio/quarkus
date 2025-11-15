package io.quarkus.redis.runtime.client.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.redis")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RedisConfig {
    String REDIS_CONFIG_ROOT_NAME = "redis";
    String HOSTS_CONFIG_NAME = "hosts";
    String DEFAULT_CLIENT_NAME = "<default>";

    /**
     * The default redis client
     */
    @WithParentName
    RedisClientConfig defaultRedisClient();

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
    Map<String, RedisClientConfig> namedRedisClients();

    static boolean isDefaultClient(String name) {
        return DEFAULT_CLIENT_NAME.equalsIgnoreCase(name);
    }

    static String propertyKey(String name, String radical) {
        String prefix = DEFAULT_CLIENT_NAME.equals(name)
                ? "quarkus.redis."
                : "quarkus.redis.\"" + name + "\".";
        return prefix + radical;
    }
}
