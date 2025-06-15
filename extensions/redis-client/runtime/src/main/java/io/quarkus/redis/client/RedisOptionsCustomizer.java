package io.quarkus.redis.client;

import io.vertx.redis.client.RedisOptions;

/**
 * Beans exposing the {@code RedisClientOptionsCustomizer} interface has the possibility to extend/modify the
 * {@link io.vertx.redis.client.RedisOptions} before they are used to create the {@code RedisClient} or
 * {@code RedisDataSource}.
 */
public interface RedisOptionsCustomizer {

    /**
     * Allows customizing the options for the client named {@code clientName}. The passed {@code options} must be
     * modified <em>in-place</em>.
     *
     * @param clientName
     *        the client name, {@link io.quarkus.redis.runtime.client.config.RedisConfig#DEFAULT_CLIENT_NAME} for
     *        the default client.
     * @param options
     *        the options.
     */
    void customize(String clientName, RedisOptions options);

}
