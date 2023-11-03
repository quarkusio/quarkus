package io.quarkus.it.vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.ManagementInterface;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class NoManagementInterfaceRoute {

    boolean exposed;

    void initMain(@Observes Router router) {
        router.get("/management-interface-test").handler(rc -> {
            if (exposed) {
                rc.response().setStatusCode(500).end("KO");
            } else {
                rc.response().end("OK");
            }
        });
    }

    void init(@Observes ManagementInterface mi) {
        exposed = true;
        mi.router().get("/admin").handler(rc -> rc.response().end("admin it is"));
    }

}
