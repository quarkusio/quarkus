package io.quarkus.undertow.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A non-generic marker interface extending Handler&lt;RoutingContext&gt; so it can be registered
 * as a service type in the Quarkus ServiceGraph without violating the non-generic constraint.
 */
public interface UndertowVertxHandler extends Handler<RoutingContext> {
}
