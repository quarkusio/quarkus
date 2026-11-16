package io.quarkus.vertx.http.crac;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.ManagementInterface;

@ApplicationScoped
public class MgmtRoute {
    void register(@Observes ManagementInterface mgmt) {
        mgmt.router().get("/ping").handler(rc -> rc.end("pong"));
    }
}
