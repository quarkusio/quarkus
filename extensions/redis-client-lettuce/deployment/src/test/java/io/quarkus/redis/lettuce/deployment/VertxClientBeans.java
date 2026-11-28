package io.quarkus.redis.lettuce.deployment;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.Any;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClientName;

/**
 * Lists the Vert.x Redis client beans of the test application, for the tests of when the Lettuce backend leaves the
 * Vert.x Redis client out.
 */
@SuppressWarnings("deprecation")
final class VertxClientBeans {

    static final String DEFAULT = "default";

    private static final List<Class<?>> TYPES = List.of(
            io.vertx.mutiny.redis.client.Redis.class,
            io.vertx.redis.client.Redis.class,
            io.vertx.mutiny.redis.client.RedisAPI.class,
            io.vertx.redis.client.RedisAPI.class,
            io.quarkus.redis.client.RedisClient.class,
            io.quarkus.redis.client.reactive.ReactiveRedisClient.class);

    private VertxClientBeans() {
    }

    /**
     * @return the names of the clients that have a Vert.x Redis client bean of any type, {@link #DEFAULT} for the
     *         default client
     */
    static Set<String> clientNames() {
        Set<String> names = new TreeSet<>();
        for (Class<?> type : TYPES) {
            for (InstanceHandle<?> handle : Arc.container().listAll(type, Any.Literal.INSTANCE)) {
                names.add(clientName(handle));
            }
        }
        return names;
    }

    private static String clientName(InstanceHandle<?> handle) {
        for (Annotation qualifier : handle.getBean().getQualifiers()) {
            if (qualifier instanceof RedisClientName redisClientName) {
                return redisClientName.value();
            }
        }
        return DEFAULT;
    }
}
