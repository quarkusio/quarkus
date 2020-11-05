package io.quarkus.smallrye.openapi.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handling not found when disabled
 */
public class OpenApiNotFoundHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        event.response().setStatusCode(404);
        event.response().end();
    }

}
