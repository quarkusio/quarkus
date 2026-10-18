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
    private final Map<String, Consumer<RoutingContext>> classNameToInterceptor;

    EagerSecurityInterceptorStorage(
            Map<MethodDescription, Consumer<RoutingContext>> methodToInterceptor,
            Map<String, Consumer<RoutingContext>> classNameToInterceptor) {
        this.methodToInterceptor = methodToInterceptor.isEmpty() ? Map.of() : Map.copyOf(methodToInterceptor);
        this.classNameToInterceptor = classNameToInterceptor.isEmpty() ? Map.of() : Map.copyOf(classNameToInterceptor);
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

    /**
     * This method should be invoked prior to any security check is run if proactive auth is disabled.
     * Class-level security interceptors are applied when security is applied once per class, for example per HTTP upgrade.
     *
     * @param className with security annotation
     * @return return class security interceptor
     */
    public Consumer<RoutingContext> getClassInterceptor(String className) {
        return classNameToInterceptor.get(className);
    }
}
