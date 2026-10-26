package io.quarkus.vertx.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A non-generic Handler for Vert.x RoutingContext to support
 * ServiceRegistrar service registration without generic types.
 */
public interface RouteHandler extends Handler<RoutingContext> {
}
