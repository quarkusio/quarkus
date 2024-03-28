package io.quarkus.vertx.http.runtime.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects {@link FormAuthenticationMechanism}.
 * Equivalent to '@HttpAuthMechanism("form")'.
 *
 * @see HttpAuthMechanism for more information
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Form {

    String AUTH_MECHANISM_SCHEME = "form";

}
