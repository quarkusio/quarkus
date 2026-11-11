package io.quarkus.vertx.http.crac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.bootstrap.runner.CracSupport;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class CracRoute {

    void register(@Observes Router router) {
        router.get("/crac").handler(rc -> rc.end(String.valueOf(CracSupport.isEnabled())));
    }
}
