package io.quarkus.redis.client.runtime;

import java.net.InetSocketAddress;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

class RedisAPIProducer {
    private static final char AT = '@';
    private static final char COLON = ':';
    private static final char SLASH = '/';
    private static final String REDIS_SCHEME = "redis://";
    private static final String REDIS_SSL_SCHEME = "rediss://";

    private long timeout = 10;

    private final RedisConfig config;

    @Produces
    @ApplicationScoped
    private final Redis vertxRedisClient;

    @Produces
    @ApplicationScoped
    private final RedisAPI redisAPI;

    @Produces
    @ApplicationScoped
    private final RedisClient redisClient;

    @Produces
    @ApplicationScoped
    private final ReactiveRedisClient reactiveClient;

    @Produces
    @ApplicationScoped
    private final MutinyRedisClient mutinyRedisClient;

    @Produces
    @ApplicationScoped
    private final MutinyRedisClientAPI mutinyRedisClientAPI;

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

        options.setMaxPoolSize(config.maxPoolSize);
        options.setMaxPoolWaiting(config.maxPoolWaiting);
        options.setPoolRecycleTimeout(Math.toIntExact(config.poolRecycleTimeout.toMillis()));
        if (config.poolCleanerInterval.isPresent()) {
            options.setPoolCleanerInterval(Math.toIntExact(config.poolCleanerInterval.get().toMillis()));
        }

        vertxRedisClient = Redis.createClient(vertx, options);
        redisAPI = RedisAPI.api(vertxRedisClient);
        mutinyRedisClient = new MutinyRedisClient(vertxRedisClient);
        mutinyRedisClientAPI = new MutinyRedisClientAPI(redisAPI);
        redisClient = new RedisClientImpl(mutinyRedisClientAPI, timeout);
        reactiveClient = new ReactiveRedisClientImpl(mutinyRedisClientAPI);
    }

    @PreDestroy
    public void close() {
        this.redisAPI.close();
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

    static class MutinyRedisClient extends io.vertx.mutiny.redis.client.Redis {
        MutinyRedisClient(Redis delegate) {
            super(delegate);
        }

        MutinyRedisClient() {
            super(null);
        }
    }

    static class MutinyRedisClientAPI extends io.vertx.mutiny.redis.client.RedisAPI {
        MutinyRedisClientAPI(RedisAPI delegate) {
            super(delegate);
        }

        MutinyRedisClientAPI() {
            super(null);
        }
    }
}
