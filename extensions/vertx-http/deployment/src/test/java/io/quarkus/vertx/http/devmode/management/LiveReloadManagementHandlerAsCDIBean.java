package io.quarkus.vertx.http.devmode.management;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Named
@ApplicationScoped
public class LiveReloadManagementHandlerAsCDIBean implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        event.response().end("I'm a CDI bean handler.");
    }
}
