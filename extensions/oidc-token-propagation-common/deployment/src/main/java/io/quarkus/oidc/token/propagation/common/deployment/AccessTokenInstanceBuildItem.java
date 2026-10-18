package io.quarkus.oidc.token.propagation.common.deployment;

import java.util.Objects;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents one {@link io.quarkus.oidc.token.propagation.common.AccessToken} annotation instance.
 */
public final class AccessTokenInstanceBuildItem extends MultiBuildItem {

    private final String clientName;
    private final boolean tokenExchange;
    private final AnnotationTarget annotationTarget;
    private final MethodInfo targetMethodInfo;

    AccessTokenInstanceBuildItem(String clientName, Boolean tokenExchange, AnnotationTarget annotationTarget,
            MethodInfo targetMethodInfo) {
        this.clientName = Objects.requireNonNull(clientName);
        this.tokenExchange = tokenExchange;
        this.annotationTarget = Objects.requireNonNull(annotationTarget);
        this.targetMethodInfo = targetMethodInfo;
    }

    String getClientName() {
        return clientName;
    }

    boolean exchangeTokenActivated() {
        return tokenExchange;
    }

    public AnnotationTarget getAnnotationTarget() {
        return annotationTarget;
    }

    public String targetClass() {
        if (annotationTarget.kind() == AnnotationTarget.Kind.CLASS) {
            return annotationTarget.asClass().name().toString();
        }
        return annotationTarget.asMethod().declaringClass().name().toString();
    }

    MethodInfo getTargetMethodInfo() {
        return targetMethodInfo;
    }
}
