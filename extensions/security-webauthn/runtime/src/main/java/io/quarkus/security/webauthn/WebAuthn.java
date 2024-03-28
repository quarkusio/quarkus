package io.quarkus.security.webauthn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.HttpAuthMechanism;

/**
 * Selects {@link WebAuthnAuthenticationMechanism}.
 * Equivalent to '@HttpAuthMechanism("webauthn")'.
 *
 * @see HttpAuthMechanism for more information
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface WebAuthn {

    String AUTH_MECHANISM_SCHEME = "webauthn";

}
