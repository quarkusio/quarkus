package io.quarkus.it.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

@ApplicationScoped
public class BeanRegisteringRoute {

    void init(@Observes Router router) {
        router.route("/my-path").handler(rc -> rc.response().end("OK"));
    }
}
