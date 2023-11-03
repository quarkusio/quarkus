package io.quarkus.vertx.http.deployment;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.vertx.ext.web.RoutingContext;

/**
 * Bears collected security interceptors per method candidate. Methods are candidates because not each of them
 * must be finally resolved to endpoint and invoked.
 * <p>
 * This build item should be consumed by every extension that run {@link io.quarkus.security.spi.runtime.SecurityCheck}s
 * before CDI interceptors when proactive auth is disabled.
 *
 * @see EagerSecurityInterceptorCandidateBuildItem for detailed information on security filters
 */
public final class EagerSecurityInterceptorBuildItem extends SimpleBuildItem {

    private final List<MethodInfo> methodCandidates;
    final Map<RuntimeValue<MethodDescription>, Consumer<RoutingContext>> methodCandidateToSecurityInterceptor;

    EagerSecurityInterceptorBuildItem(
            List<MethodInfo> methodCandidates,
            Map<RuntimeValue<MethodDescription>, Consumer<RoutingContext>> methodCandidateToSecurityInterceptor) {
        this.methodCandidates = methodCandidates;
        this.methodCandidateToSecurityInterceptor = Map.copyOf(methodCandidateToSecurityInterceptor);
    }

    public boolean applyInterceptorOn(MethodInfo method) {
        return methodCandidates.contains(method);
    }
}
