package io.quarkus.vertx.http.deployment;

import java.util.Objects;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpAuthMechanism;

/**
 * Register {@link io.quarkus.vertx.http.runtime.security.HttpAuthMechanism} meta annotations.
 * This way, users can use {@link io.quarkus.vertx.http.runtime.security.Basic} instead of '@HttpAuthMechanism("basic")'.
 */
public final class HttpAuthMechanismAnnotationBuildItem extends MultiBuildItem {

    /**
     * Annotation name, for example {@link io.quarkus.vertx.http.runtime.security.Basic}.
     */
    final DotName annotationName;
    /**
     * Authentication mechanism scheme, as defined by {@link HttpAuthMechanism#value()}.
     */
    final String authMechanismScheme;

    public HttpAuthMechanismAnnotationBuildItem(DotName annotationName, String authMechanismScheme) {
        this.annotationName = Objects.requireNonNull(annotationName);
        this.authMechanismScheme = Objects.requireNonNull(authMechanismScheme);
    }
}
