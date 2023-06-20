package io.quarkus.test.security.jwt;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Claim {
    /**
     * Claim name
     */
    String key();

    /**
     * Claim value
     */
    String value();

    /**
     * Claim value type, the value will be converted to String if this type is set to Object.class.
     * Supported types: String, Integer, int, Long, long, Boolean, boolean, jakarta.json.JsonArray, jakarta.json.JsonObject.
     */
    Class<?> type() default Object.class;
}
