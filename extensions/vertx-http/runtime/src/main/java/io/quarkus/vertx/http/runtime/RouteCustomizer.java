package io.quarkus.vertx.http.runtime;

import java.util.function.Consumer;

import io.vertx.ext.web.Route;

/**
 * A non-generic Consumer for Vert.x Route to support
 * ServiceRegistrar service registration without generic types.
 */
public interface RouteCustomizer extends Consumer<Route> {
}
