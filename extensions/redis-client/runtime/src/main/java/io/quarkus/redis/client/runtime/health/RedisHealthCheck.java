package io.quarkus.redis.client.runtime.health;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClient;

@Readiness
@ApplicationScoped
class RedisHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();

        try (InstanceHandle<RedisClient> instanceHandle = Arc.container().instance(RedisClient.class)) {
            if (!instanceHandle.isAvailable()) {
                builder.down();
            } else {
                RedisClient redisAPI = instanceHandle.get();
                redisAPI.ping(Collections.emptyList());
            }
        } catch (RuntimeException e) {
            builder.down();
        }
        return builder.build();
    }
}
