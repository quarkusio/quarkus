package io.quarkus.redis.client.runtime.health;

import static io.quarkus.redis.client.runtime.RedisClientUtil.DEFAULT_CLIENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

@Readiness
@ApplicationScoped
class RedisHealthCheck implements HealthCheck {
    private Map<String, RedisClient> clients = new HashMap<>();

    @PostConstruct
    protected void init() {
        Set<Bean<?>> beans = Arc.container().beanManager().getBeans(RedisClient.class);
        for (Bean<?> bean : beans) {
            if (bean.getName() == null) {
                // this is the default redis client: retrieve it by type
                RedisClient defaultClient = Arc.container().instance(RedisClient.class).get();
                clients.put(DEFAULT_CLIENT, defaultClient);
            } else {
                RedisClient client = (RedisClient) Arc.container().instance(bean.getName()).get();
                clients.put(bean.getName(), client);
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, RedisClient> client : clients.entrySet()) {
            boolean isDefault = DEFAULT_CLIENT.equals(client.getKey());
            RedisClient redisClient = client.getValue();
            try {
                String redisClientName = isDefault ? "default" : client.getKey();
                Response response = redisClient.ping(Collections.emptyList());
                builder.up().withData(redisClientName, response.toString());
            } catch (Exception e) {
                return builder.down().withData("reason", e.getMessage()).build();
            }
        }
        return builder.build();
    }
}
