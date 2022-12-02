package io.quarkus.vertx.http.devmode;

import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

public class CompileCorrectlyEndpoint {

    void addConfigRoute(@Observes Router router) {
        router.route("/correct")
                .produces("text/plain")
                .handler(rc -> rc.response().end("correct"));
    }
}
