package io.quarkus.vertx.http.devmode;

import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

public class CompileErrorEndpoint {

    void addConfigRoute(@Observes Router router) {
        router.route("/error")
                .produces("text/plain")
                .handler(rc -> rc.response().end("error"));
    }
}
