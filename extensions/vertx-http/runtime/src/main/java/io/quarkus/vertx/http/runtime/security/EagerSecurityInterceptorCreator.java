package io.quarkus.vertx.http.runtime.security;

import java.util.function.Consumer;
import java.util.function.Function;

import io.vertx.ext.web.RoutingContext;

/**
 * Creates an eager security interceptor for the value of an annotation binding.
 * A non-generic Function to support ServiceRegistrar service registration without generic types.
 */
public interface EagerSecurityInterceptorCreator extends Function<String, Consumer<RoutingContext>> {
}
