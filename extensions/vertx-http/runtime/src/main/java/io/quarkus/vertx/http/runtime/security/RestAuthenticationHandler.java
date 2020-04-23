package io.quarkus.vertx.http.runtime.security;

import javax.enterprise.inject.spi.CDI;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RestAuthenticationHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        if (event.request().method().equals(HttpMethod.POST)) {
            JsonObject body = event.getBodyAsJson();
            String username = body.getString("username");
            String password = body.getString("password");
            CDI.current().select(HttpAuthenticator.class);
        }
        event.next();
    }
}
