package io.quarkus.redis.client.runtime;

import java.net.InetSocketAddress;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

@ApplicationScoped
class RedisAPIProducer {
    private static final char AT = '@';
    private static final char COLON = ':';
    private static final char SLASH = '/';
    private static final String REDIS_SCHEME = "redis://";
    private static final String REDIS_SSL_SCHEME = "rediss://";

    private long timeout = 10;

    private final RedisConfig config;

    private final Redis vertxRedisClient;

    private final RedisAPI redisAPI;

    private final RedisClient redisClient;

    private final ReactiveRedisClient reactiveClient;

    private final io.vertx.mutiny.redis.client.Redis mutinyRedisClient;

    private final io.vertx.mutiny.redis.client.RedisAPI mutinyRedisAPI;

    public RedisAPIProducer(RedisConfig config, Vertx vertx) {
        this.config = config;
        RedisOptions options = new RedisOptions();
        options.setType(config.clientType);

        if (RedisClientType.STANDALONE == config.clientType) {
            if (config.hosts.isPresent() && config.hosts.get().size() > 1) {
                throw new ConfigurationException("Multiple hosts supplied for non clustered configuration");
            }
        }

        if (config.hosts.isPresent()) {
            Set<InetSocketAddress> hosts = config.hosts.get();
            for (InetSocketAddress host : hosts) {
                String connectionString = buildConnectionString(host);
                options.addConnectionString(connectionString);
            }
        } else {
            InetSocketAddress defaultRedisAddress = new InetSocketAddress("localhost", 6379);
            String connectionString = buildConnectionString(defaultRedisAddress);
            options.addConnectionString(connectionString);
        }

        if (config.timeout.isPresent()) {
            timeout = config.timeout.get().getSeconds();
        }

        vertxRedisClient = Redis.createClient(vertx, options);
        redisAPI = RedisAPI.api(vertxRedisClient);
        mutinyRedisClient = io.vertx.mutiny.redis.client.Redis.newInstance(vertxRedisClient);
        mutinyRedisAPI = io.vertx.mutiny.redis.client.RedisAPI.api(mutinyRedisClient);
        redisClient = new RedisClientImpl(mutinyRedisAPI, timeout);
        reactiveClient = new ReactiveRedisClientImpl(mutinyRedisAPI);
    }

    @Produces
    @Singleton
    Redis redis() {
        return vertxRedisClient;
    }

    @Produces
    @Singleton
    RedisAPI redisAPI() {
        return redisAPI;
    }

    @Produces
    @Singleton
    RedisClient redisClient() {
        return redisClient;
    }

    @Produces
    @Singleton
    ReactiveRedisClient reactiveRedisClient() {
        return reactiveClient;
    }

    @Produces
    @Singleton
    io.vertx.mutiny.redis.client.Redis mutinyRedisClient() {
        return mutinyRedisClient;
    }

    @Produces
    @Singleton
    io.vertx.mutiny.redis.client.RedisAPI mutinyRedisAPI() {
        return mutinyRedisAPI;
    }

    @PreDestroy
    public void close() {
        this.redis().close();
    }

    private String buildConnectionString(InetSocketAddress address) {
        final StringBuilder builder = config.ssl ? new StringBuilder(REDIS_SSL_SCHEME) : new StringBuilder(REDIS_SCHEME);

        if (config.password.isPresent()) {
            builder.append(config.password.get());
            builder.append(AT);
        }

        builder.append(address.getHostString());
        builder.append(COLON);
        builder.append(address.getPort());
        builder.append(SLASH);
        builder.append(config.database);

        return builder.toString();
    }
}
