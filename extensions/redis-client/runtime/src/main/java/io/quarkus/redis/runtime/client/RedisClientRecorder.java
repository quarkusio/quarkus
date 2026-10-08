package io.quarkus.redis.runtime.client;

import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Recorder for Redis client beans.
 * <p>
 * This recorder is intentionally minimal: it exists only to produce
 * {@link ActiveResult} suppliers for synthetic bean activation checks.
 * All client creation, lifecycle management, and data preloading are
 * handled by the service graph (see {@code RedisClientProcessor}).
 */
@Recorder
public class RedisClientRecorder {

    private final RuntimeValue<RedisConfig> runtimeConfig;

    /**
     * Construct a new recorder.
     *
     * @param runtimeConfig the runtime Redis configuration
     */
    public RedisClientRecorder(RuntimeValue<RedisConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Produce a supplier that checks whether the named Redis client is active
     * (configured and enabled). Used by synthetic beans to gate activation.
     *
     * @param name the Redis client name (must not be {@code null})
     * @return a supplier that returns the activation result
     */
    public Supplier<ActiveResult> checkActive(final String name) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                RedisClientConfig redisClientConfig = runtimeConfig.getValue().clients().get(name);
                if (!redisClientConfig.active()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Redis Client '%s' was deactivated through configuration properties. \
                                    To activate the Redis Client, set configuration property '%s' to 'true' and configure the Redis Client '%s'. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """,
                            name, getPropertyName(name, "active"), name));
                }
                if (redisClientConfig.hosts().isEmpty() && redisClientConfig.hostsProviderName().isEmpty()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Redis Client '%s' was deactivated automatically because neither the hosts nor the hostsProviderName is set. \
                                    To activate the Redis Client, set the configuration property '%s' or '%s'. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """,
                            name, getPropertyName(name, HOSTS), getPropertyName(name, HOSTS_PROVIDER_NAME)));
                }
                return ActiveResult.active();
            }
        };
    }
}
