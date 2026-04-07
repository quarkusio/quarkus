package io.quarkus.reactive.pg.client;

import java.net.ConnectException;

import jakarta.inject.Inject;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;

public class DevModeResource {

    @Inject
    Pool client;

    @Route(path = "/dev/error", methods = Route.HttpMethod.GET)
    void getErrorMessage(RoutingContext rc) {
        client.query("SELECT 1").execute().onComplete(ar -> {
            Class<?> expectedExceptionClass = ConnectException.class;
            if (ar.succeeded()) {
                rc.response().setStatusCode(500).end("Expected SQL query to fail");
            } else if (!expectedExceptionClass.isAssignableFrom(ar.cause().getClass())) {
                ar.cause().printStackTrace();
                rc.response().setStatusCode(500)
                        .end("Expected " + expectedExceptionClass + ", got " + ar.cause().getClass());
            } else {
                rc.response().setStatusCode(200).end(ar.cause().getMessage());
            }
        });
    }
}
