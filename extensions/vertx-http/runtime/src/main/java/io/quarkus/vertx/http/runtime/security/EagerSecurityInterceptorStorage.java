package io.quarkus.vertx.http.runtime.security;

import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.security.spi.runtime.MethodDescription;
import io.vertx.ext.web.RoutingContext;

/**
 * Security interceptors run for desired annotated methods prior to security checks.
 * Interceptors should only be run when proactive security is disabled, because
 * they must be run before request is authenticated.
 * <p>
 * These interceptors are very important when {@link io.quarkus.security.spi.runtime.SecurityCheck}s are not run
 * with CDI interceptors, because they provide only way to link request to invoked method prior to checks.
 */
public class EagerSecurityInterceptorStorage {

    private final Map<MethodDescription, Consumer<RoutingContext>> methodToInterceptor;

    EagerSecurityInterceptorStorage(
            Map<MethodDescription, Consumer<RoutingContext>> methodToInterceptor) {
        this.methodToInterceptor = Map.copyOf(methodToInterceptor);
    }

    /**
     * This method should be invoked prior to any security check is run if proactive auth is disabled.
     *
     * @param endpoint with security annotation
     * @return return security interceptor
     */
    public Consumer<RoutingContext> getInterceptor(MethodDescription endpoint) {
        return methodToInterceptor.get(endpoint);
    }
}
