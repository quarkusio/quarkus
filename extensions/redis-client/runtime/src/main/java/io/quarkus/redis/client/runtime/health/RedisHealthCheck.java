package io.quarkus.redis.client.runtime.health;

import static io.quarkus.redis.client.runtime.RedisClientUtil.DEFAULT_CLIENT;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.client.runtime.RedisClientUtil;
import io.quarkus.redis.client.runtime.RedisConfig;
import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;
import io.vertx.redis.client.Response;

@Readiness
@ApplicationScoped
class RedisHealthCheck implements HealthCheck {
    private final Map<String, RedisClient> clients = new HashMap<>();
    private final Map<String, ReactiveRedisClient> reactiveClients = new HashMap<>();
    private final RedisConfig redisConfig;

    public RedisHealthCheck(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    @PostConstruct
    protected void init() {
        for (InstanceHandle<RedisClient> handle : Arc.container().select(RedisClient.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getClientName(handle.getBean());
            clients.put(clientName == null ? DEFAULT_CLIENT : clientName, handle.get());
        }

        for (InstanceHandle<ReactiveRedisClient> handle : Arc.container()
                .select(ReactiveRedisClient.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getClientName(handle.getBean());
            reactiveClients.put(clientName == null ? DEFAULT_CLIENT : clientName, handle.get());
        }
    }

    private String getClientName(Bean bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof RedisClientName) {
                return ((RedisClientName) qualifier).value();
            }
        }
        return null;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, RedisClient> client : clients.entrySet()) {
            try {
                boolean isDefault = DEFAULT_CLIENT.equals(client.getKey());
                RedisClient redisClient = client.getValue();
                String redisClientName = isDefault ? "default" : client.getKey();
                Response response = redisClient.ping(Collections.emptyList());
                builder.up().withData(redisClientName, response.toString());
            } catch (Exception e) {
                return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e.getMessage()).build();
            }
        }

        for (Map.Entry<String, ReactiveRedisClient> client : reactiveClients.entrySet()) {

            // Ignore named ReactiveRedisClient that have a blocking RedisClient since they have already been checked as part of blocking clients
            if (clients.containsKey(client.getKey())) {
                continue;
            }

            try {
                boolean isDefault = DEFAULT_CLIENT.equals(client.getKey());
                ReactiveRedisClient redisClient = client.getValue();
                RedisConfiguration redisConfig = RedisClientUtil.getConfiguration(this.redisConfig,
                        isDefault ? DEFAULT_CLIENT : client.getKey());
                long timeout = 10;
                if (redisConfig.timeout.isPresent()) {
                    timeout = redisConfig.timeout.get().getSeconds();
                }
                String redisClientName = isDefault ? "default" : client.getKey();
                io.vertx.mutiny.redis.client.Response response = redisClient.ping(Collections.emptyList()).await()
                        .atMost(Duration.ofSeconds(timeout));
                builder.up().withData(redisClientName, response.toString());
            } catch (Exception e) {
                return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e.getMessage())
                        .build();
            }
        }
        return builder.build();
    }
}
