package io.quarkus.devui;

import jakarta.enterprise.event.Observes;

import io.vertx.ext.web.Router;

public class ReloadMarkerResource {

    void init(@Observes Router router) {
        router.get("/reload-marker").handler(rc -> rc.response().end("before"));
    }
}
