package io.quarkus.vertx.http.runtime.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.vertx.ext.web.RoutingContext;

/**
 * Provides a way to select {@link HttpAuthenticationMechanism} used for a REST endpoint authentication.
 * This annotation can only be used when proactive authentication is disabled. Using the annotation with
 * enabled proactive authentication will lead to build-time failure.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface HttpAuthMechanism {
    /**
     * {@link HttpAuthenticationMechanism} scheme as returned by {@link HttpCredentialTransport#getAuthenticationScheme()}.
     * Mechanisms can set this name inside {@link HttpAuthenticationMechanism#getCredentialTransport(RoutingContext)}.
     */
    String value();
}
