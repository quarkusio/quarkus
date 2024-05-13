package io.quarkus.vertx.http.runtime.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;

/**
 * Selects {@link FormAuthenticationMechanism}.
 *
 * @see HttpAuthenticationMechanism for more information
 */
@HttpAuthenticationMechanism("form")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface FormAuthentication {

    String AUTH_MECHANISM_SCHEME = "form";

}
