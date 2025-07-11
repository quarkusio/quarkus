package io.quarkus.vertx.http.testrunner.params;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class HelloResource implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        event.response().end("hello");
    }

}
