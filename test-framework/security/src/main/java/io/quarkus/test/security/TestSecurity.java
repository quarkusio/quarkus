package io.quarkus.test.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.security.identity.SecurityIdentity;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface TestSecurity {

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

    /**
     * Adds attributes to a {@link SecurityIdentity} configured by this annotation.
     * The attributes can be retrieved by the {@link SecurityIdentity#getAttributes()} method.
     */
    SecurityAttribute[] attributes() default {};

    /**
     * Selects authentication mechanism used in a path-based authentication.
     * If an HTTP Security Policy is used to enable path-based authentication,
     * then a {@link SecurityIdentity} will only be provided by this annotation if this attribute
     * matches the 'quarkus.http.auth.permission."permissions".auth-mechanism' configuration property.
     * Situation is similar when annotations are used to enable path-based authentication for Jakarta REST endpoints.
     * For example, set this attribute to 'basic' if an HTTP request to Jakarta REST endpoint annotated with the
     * {@link io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication} should be successfully authenticated.
     */
    String authMechanism() default "";
}
