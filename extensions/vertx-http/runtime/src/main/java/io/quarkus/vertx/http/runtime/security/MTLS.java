package io.quarkus.vertx.http.runtime.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects {@link MtlsAuthenticationMechanism}.
 * Equivalent to '@HttpAuthMechanism("X509")'.
 *
 * @see HttpAuthMechanism for more information
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MTLS {

    String AUTH_MECHANISM_SCHEME = "X509";

}
