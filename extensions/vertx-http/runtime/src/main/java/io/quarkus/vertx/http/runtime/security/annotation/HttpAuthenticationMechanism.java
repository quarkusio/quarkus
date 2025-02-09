package io.quarkus.vertx.http.runtime.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides a way to select custom {@link io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism}
 * used for a REST endpoint and WebSockets Next endpoint authentication.
 * This annotation can only be used when proactive authentication is disabled. Using the annotation with
 * enabled proactive authentication will lead to build-time failure.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface HttpAuthenticationMechanism {
    /**
     * {@link io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism} scheme as returned by
     * {@link HttpCredentialTransport#getAuthenticationScheme()}.
     * Custom mechanisms can set this name inside
     * {@link io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism#getCredentialTransport(RoutingContext)}.
     */
    String value();
}
