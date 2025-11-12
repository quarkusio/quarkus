package io.quarkus.redis.deployment.client;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.redis")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RedisBuildTimeConfig {
    /**
     * Configures the Redis clients.
     * <p>
     * The default client does not have a name, and it is configured as:
     *
     * <pre>
     * quarkus.redis.hosts = redis://localhost:6379
     * </pre>
     *
     * And then use {@link jakarta.inject.Inject} to inject the client:
     *
     * <pre>
     * &#64;Inject
     * RedisAPI redis;
     * </pre>
     *
     * <p>
     * Named clients must be identified to select the right client:
     *
     * <pre>
     * quarkus.redis.client1.hosts = redis://localhost:6379
     * quarkus.redis.client2.hosts = redis://localhost:6380
     * </pre>
     *
     * And then use the {@link io.quarkus.redis.client.RedisClientName} annotation to select any of the beans:
     * <ul>
     * <li>{@link io.vertx.mutiny.redis.client.Redis}</li>
     * <li>{@link io.vertx.redis.client.Redis}</li>
     * <li>{@link io.vertx.mutiny.redis.client.RedisAPI}</li>
     * <li>{@link io.vertx.redis.client.RedisAPI}</li>
     * <li>{@link io.quarkus.redis.datasource.RedisDataSource}</li>
     * <li>{@link io.quarkus.redis.datasource.ReactiveRedisDataSource}</li>
     * </ul>
     * And inject the client:
     *
     * <pre>
     * &#64;RedisClientName("client1")
     * &#64;Inject
     * RedisAPI redis;
     * </pre>
     */
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DEFAULT_CLIENT_NAME)
    @ConfigDocMapKey("redis-client-name")
    Map<String, RedisClientBuildTimeConfig> clients();

    /**
     * Returns a {@code List} of Redis Client names. The first element of the list is the default Redis Client if
     * available. The remaining order is unspecified.
     *
     * @return a {@code List} of Redis Client names
     */
    default List<String> clientsNames() {
        List<String> names = new ArrayList<>();
        if (clients().containsKey(DEFAULT_CLIENT_NAME)) {
            names.add(DEFAULT_CLIENT_NAME);
        }
        for (String name : clients().keySet()) {
            if (name.equals(DEFAULT_CLIENT_NAME)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();
}
