package io.quarkus.reactive.oracle.client;

import jakarta.inject.Inject;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.oracleclient.OracleException;
import io.vertx.sqlclient.Pool;

public class DevModeResource {

    @Inject
    Pool client;

    @Route(path = "/dev/error", methods = Route.HttpMethod.GET)
    void checkConnectionFailure(RoutingContext rc) {
        client.query("SELECT 1 FROM DUAL").execute().onComplete(ar -> {
            Class<?> expectedExceptionClass = OracleException.class;
            if (ar.succeeded()) {
                rc.response().setStatusCode(500).end("Expected SQL query to fail");
            } else if (!expectedExceptionClass.isAssignableFrom(ar.cause().getClass())) {
                rc.response().setStatusCode(500)
                        .end("Expected " + expectedExceptionClass + ", got " + ar.cause().getClass());
            } else {
                rc.response().setStatusCode(200).end();
            }
        });
    }

    @Route(path = "/dev/connected", methods = Route.HttpMethod.GET)
    void checkConnectionSuccess(RoutingContext rc) {
        client.query("SELECT 1 FROM DUAL").execute().onComplete(ar -> {
            if (ar.succeeded()) {
                rc.response().setStatusCode(200).end();
            } else {
                rc.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }
}
