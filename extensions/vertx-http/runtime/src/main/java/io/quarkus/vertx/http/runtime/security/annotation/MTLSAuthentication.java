package io.quarkus.vertx.http.runtime.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;

/**
 * Selects {@link MtlsAuthenticationMechanism}.
 *
 * @see HttpAuthenticationMechanism for more information
 */
@HttpAuthenticationMechanism("X509")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MTLSAuthentication {

    String AUTH_MECHANISM_SCHEME = "X509";

}
