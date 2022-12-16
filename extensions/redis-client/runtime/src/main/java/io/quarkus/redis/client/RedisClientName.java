package io.quarkus.redis.client;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

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

    class Literal extends AnnotationLiteral<RedisClientName> implements RedisClientName {

        public static Literal of(String value) {
            return new Literal(value);
        }

        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
