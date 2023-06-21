package io.quarkus.test.security.oidc;

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

    enum Type {
        LONG,
        INTEGER,
        BOOLEAN,
        STRING,
        JSONARRAY,
        JSONOBJECT,
        UNKNOWN
    }

    /**
     * Claim value type.
     * If this type is set to {@link Type#UNKNOWN} then the value will be converted to String unless the claim
     * is a standard claim such as `exp` (expiry), `iat` (issued at), `nbf` (not before), `auth_time` (authentication time)
     * whose value will be converted to Long or `email_verified` whose value will be converted to Boolean.
     */
    Type type() default Type.UNKNOWN;
}
