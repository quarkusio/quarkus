package io.quarkus.redis.runtime.client.health;

import static io.quarkus.redis.runtime.client.VertxRedisClientFactory.DEFAULT_CLIENT;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.smallrye.mutiny.TimeoutException;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

@Readiness
@ApplicationScoped
class RedisHealthCheck implements HealthCheck {
    private final Map<String, Redis> clients = new HashMap<>();

    private final RedisConfig config;

    public RedisHealthCheck(RedisConfig config) {
        this.config = config;
    }

    @PostConstruct
    protected void init() {
        for (InstanceHandle<Redis> handle : Arc.container().select(Redis.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getClientName(handle.getBean());
            clients.putIfAbsent(clientName == null ? DEFAULT_CLIENT : clientName, handle.get());
        }

        for (InstanceHandle<ReactiveRedisDataSource> handle : Arc.container()
                .select(ReactiveRedisDataSource.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getClientName(handle.getBean());
            Redis redis = handle.get().getRedis();
            clients.putIfAbsent(clientName == null ? DEFAULT_CLIENT : clientName, redis);
        }

        for (InstanceHandle<RedisDataSource> handle : Arc.container().select(RedisDataSource.class, Any.Literal.INSTANCE)
                .handles()) {
            String clientName = getClientName(handle.getBean());
            Redis redis = handle.get().getReactive().getRedis();
            clients.putIfAbsent(clientName == null ? DEFAULT_CLIENT : clientName, redis);
        }
    }

    private String getClientName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof RedisClientName) {
                return ((RedisClientName) qualifier).value();
            }
        }
        return null;
    }

    private Duration getTimeout(String name) {
        if (RedisConfig.isDefaultClient(name)) {
            return config.defaultRedisClient.timeout;
        } else {
            return config.namedRedisClients.get(name).timeout;
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, Redis> client : clients.entrySet()) {
            try {
                boolean isDefault = DEFAULT_CLIENT.equals(client.getKey());
                Redis redisClient = client.getValue();
                String redisClientName = isDefault ? "default" : client.getKey();
                Duration timeout = getTimeout(client.getKey());
                Response response = redisClient.send(Request.cmd(Command.PING)).await().atMost(timeout);
                builder.up().withData(redisClientName, response.toString());
            } catch (TimeoutException e) {
                return builder.down().withData("reason", "client [" + client.getKey() + "]: timeout").build();
            } catch (Exception e) {
                if (e.getMessage() == null) {
                    return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e).build();
                }
                return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e.getMessage()).build();
            }
        }
        return builder.build();
    }
}
