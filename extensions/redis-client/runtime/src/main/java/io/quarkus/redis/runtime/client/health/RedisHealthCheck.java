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

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

@Readiness
@ApplicationScoped
class RedisHealthCheck implements HealthCheck {
    private final Map<String, Redis> clients = new HashMap<>();
    // The data sources of the clients that have no Vert.x client: another backend provides them
    private final Map<String, ReactiveRedisDataSource> dataSources = new HashMap<>();

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
        // Not an injection point: the health check must not make the data sources of every configured client exist
        for (InstanceHandle<ReactiveRedisDataSource> handle : Arc.container().listAll(ReactiveRedisDataSource.class,
                Any.Literal.INSTANCE)) {
            String clientName = getClientName(handle.getBean());
            if (handle.getBean().isActive() && !clients.containsKey(clientName)) {
                dataSources.putIfAbsent(clientName, handle.get());
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, Redis> client : clients.entrySet()) {
            String clientName = client.getKey().equals(DEFAULT_CLIENT_NAME) ? "default" : client.getKey();
            Redis redisClient = client.getValue();
            ping(builder, clientName, redisClient.send(Request.cmd(Command.PING)));
        }
        for (Map.Entry<String, ReactiveRedisDataSource> dataSource : dataSources.entrySet()) {
            String clientName = dataSource.getKey().equals(DEFAULT_CLIENT_NAME) ? "default" : dataSource.getKey();
            ping(builder, clientName, dataSource.getValue().execute(Command.PING));
        }
        return builder.build();
    }

    private void ping(HealthCheckResponseBuilder builder, String clientName, Uni<Response> ping) {
        Duration timeout = config.clients().get(clientName).timeout();
        try {
            Response response = ping.await().atMost(timeout);
            // the builder starts up: a client that answers must not put it back up after another one failed
            builder.withData(clientName, response.toString());
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

    private static String getClientName(final Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof RedisClientName redisClientName) {
                return redisClientName.value();
            }
        }
        return DEFAULT_CLIENT_NAME;
    }
}
