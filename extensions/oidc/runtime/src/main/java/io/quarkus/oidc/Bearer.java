package io.quarkus.oidc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.HttpAuthMechanism;

/**
 * Selects {@link io.quarkus.oidc.runtime.BearerAuthenticationMechanism}.
 * Equivalent to '@HttpAuthMechanism("Bearer")'.
 *
 * @see HttpAuthMechanism for more information
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Bearer {

}
