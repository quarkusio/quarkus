package io.quarkus.logging.manager.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handling static when disabled
 */
public class LoggingManagerNotFoundHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        event.response().setStatusCode(404);
        event.response().end();
    }

}
