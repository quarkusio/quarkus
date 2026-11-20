package io.quarkus.vertx.http.runtime.security;

import java.util.function.Consumer;
import java.util.function.Function;

import io.vertx.ext.web.RoutingContext;

/**
 * Creates eager security interceptors for the annotation instances of an interceptor binding.
 * <p>
 * Register the creator as a static-init service of this type named with the {@link Class#getName() name} of the
 * interceptor binding annotation. The Vert.x HTTP extension uses the creator when it creates the
 * {@link EagerSecurityInterceptorStorage}.
 *
 * @param interceptorCreator accepts the binding value of an annotation instance and creates the interceptor
 */
public record EagerSecurityInterceptorCreator(Function<String, Consumer<RoutingContext>> interceptorCreator) {
}
