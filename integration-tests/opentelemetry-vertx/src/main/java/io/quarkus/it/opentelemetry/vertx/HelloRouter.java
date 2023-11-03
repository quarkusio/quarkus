package io.quarkus.it.opentelemetry.vertx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class HelloRouter {
    @Inject
    Router router;
    @Inject
    EventBus eventBus;

    List<String> messages = new CopyOnWriteArrayList<>();

    public void register(@Observes StartupEvent ev) {
        router.get("/hello").handler(rc -> rc.response().end("hello"));
        router.get("/hello/:name").handler(rc -> rc.response().end("hello " + rc.pathParam("name")));
        router.post("/hello").handler(BodyHandler.create()).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(final RoutingContext rc) {
                rc.response().end("hello " + rc.getBodyAsString());
            }
        });

        router.get("/bus").handler(rc -> {
            eventBus.publish("bus", "hello to bus");
            rc.response().end("hello");
        });
        router.get("/bus/messages").handler(rc -> rc.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(messages)));
    }

    @ConsumeEvent("bus")
    public void onBusEvent(final String msg) {
        messages.add(msg);
    }
}
