package io.quarkus.vertx.http.runtime.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;

/**
 * Selects {@link BasicAuthenticationMechanism}.
 *
 * @see HttpAuthenticationMechanism for more information
 */
@HttpAuthenticationMechanism("basic")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface BasicAuthentication {

    String AUTH_MECHANISM_SCHEME = "basic";

}
