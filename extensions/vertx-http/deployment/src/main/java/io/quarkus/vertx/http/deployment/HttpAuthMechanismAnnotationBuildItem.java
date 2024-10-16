package io.quarkus.vertx.http.deployment;

import java.util.Objects;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Register {@link HttpAuthenticationMechanism} meta annotations.
 * This way, users can use {@link BasicAuthentication} instead of '@HttpAuthenticationMechanism("basic")'.
 */
public final class HttpAuthMechanismAnnotationBuildItem extends MultiBuildItem {

    /**
     * Annotation name, for example {@link BasicAuthentication}.
     */
    final DotName annotationName;
    /**
     * Authentication mechanism scheme, as defined by {@link HttpAuthenticationMechanism#value()}.
     */
    final String authMechanismScheme;

    public HttpAuthMechanismAnnotationBuildItem(DotName annotationName, String authMechanismScheme) {
        this.annotationName = Objects.requireNonNull(annotationName);
        this.authMechanismScheme = Objects.requireNonNull(authMechanismScheme);
    }
}
