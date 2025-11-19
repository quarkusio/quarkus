package io.quarkus.redis.runtime.client.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.redis")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RedisConfig {
    String DEFAULT_CLIENT_NAME = "<default>";
    String HOSTS = "hosts";
    String HOSTS_PROVIDER_NAME = "hosts-provider-name";

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
    Map<String, RedisClientConfig> clients();

    static boolean isDefaultClient(final String name) {
        return DEFAULT_CLIENT_NAME.equalsIgnoreCase(name);
    }

    static String getPropertyName(final String name, final String attribute) {
        String prefix = DEFAULT_CLIENT_NAME.equals(name)
                ? "quarkus.redis."
                : "quarkus.redis." + (name.contains(".") ? "\"" + name + "\"" : name) + ".";
        return prefix + attribute;
    }
}
