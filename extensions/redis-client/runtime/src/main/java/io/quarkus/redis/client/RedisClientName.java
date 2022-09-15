package io.quarkus.redis.client;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Marker annotation to select the Redis client.
 *
 * For example, if the Redis connection is configured like so in {@code application.properties}:
 *
 * <pre>
 * quarkus.redis.client1.hosts=localhost:6379
 * </pre>
 *
 * Then to inject the proper {@code redisClient}, you would need to use {@code RedisClientName} like indicated below:
 *
 * <pre>
 *     &#64Inject
 *     &#64RedisClientName("client1")
 *     RedisClient client;
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface RedisClientName {
    /**
     * The Redis client name.
     */
    String value() default "";
}
