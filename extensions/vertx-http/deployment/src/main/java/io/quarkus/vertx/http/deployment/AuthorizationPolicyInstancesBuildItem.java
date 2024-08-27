package io.quarkus.vertx.http.deployment;

import java.util.Map;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries all gathered {@link io.quarkus.vertx.http.security.AuthorizationPolicy} instances that should be applied.
 */
public final class AuthorizationPolicyInstancesBuildItem extends SimpleBuildItem {

    /**
     * Contains:
     * - Methods annotated with {@link io.quarkus.vertx.http.security.AuthorizationPolicy}
     * - Methods of classes annotated with {@link io.quarkus.vertx.http.security.AuthorizationPolicy} that
     * doesn't have another standard security annotation.
     */
    final Map<MethodInfo, String> methodToPolicyName;

    AuthorizationPolicyInstancesBuildItem(Map<MethodInfo, String> methodToPolicyName) {
        this.methodToPolicyName = Map.copyOf(methodToPolicyName);
    }

    public boolean applyAuthorizationPolicy(MethodInfo methodInfo) {
        return methodToPolicyName.containsKey(methodInfo);
    }
}
