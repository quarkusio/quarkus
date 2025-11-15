package io.quarkus.redis.runtime.client.health;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClientName;
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

    @Inject
    RedisConfig config;
    @Inject
    @Any
    InjectableInstance<Redis> redis;

    @PostConstruct
    protected void init() {
        for (InstanceHandle<Redis> handle : redis.handles()) {
            if (handle.getBean().isActive()) {
                clients.putIfAbsent(getClientName(handle.getBean()), handle.get());
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, Redis> client : clients.entrySet()) {
            String clientName = client.getKey().equals(DEFAULT_CLIENT_NAME) ? "default" : client.getKey();
            Redis redisClient = client.getValue();
            Duration timeout = config.clients().get(clientName).timeout();
            try {
                Response response = redisClient.send(Request.cmd(Command.PING)).await().atMost(timeout);
                builder.up().withData(clientName, response.toString());
            } catch (TimeoutException e) {
                builder.down().withData(clientName,
                        "Unable to execute the validation check for the Redis Client due to timeout");
            } catch (Exception e) {
                if (e.getMessage() == null) {
                    builder.down().withData(clientName, "Unable to execute the validation check for the Redis Client: " + e);
                } else {
                    builder.down().withData(clientName,
                            "Unable to execute the validation check for the Redis Client: " + e.getMessage());
                }
            }
        }
        return builder.build();
    }

    private static String getClientName(final Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof RedisClientName redisClientName) {
                return redisClientName.value();
            }
        }
        return DEFAULT_CLIENT_NAME;
    }
}
