package io.quarkus.test.security.oidc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.security.common.SecurityAttribute;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TestOidcSecurity {

    /**
     * If this is false then all security constraints are disabled.
     */
    boolean authorizationEnabled() default true;

    /**
     * If this is non-zero then the test will be run with a SecurityIdentity with the specified username.
     */
    String user() default "";

    /**
     * Used in combination with {@link #user()} to specify the users roles.
     */
    String[] roles() default {};

    SecurityAttribute[] attributes() default {};

    Claim[] claims() default {};

    UserInfo[] userinfo() default {};

    ConfigMetadata[] config() default {};
}
