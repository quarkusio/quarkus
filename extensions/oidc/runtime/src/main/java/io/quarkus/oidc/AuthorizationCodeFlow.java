package io.quarkus.oidc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.oidc.runtime.CodeAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Selects {@link CodeAuthenticationMechanism}.
 *
 * @see HttpAuthenticationMechanism for more information
 */
@HttpAuthenticationMechanism("code")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface AuthorizationCodeFlow {

}
