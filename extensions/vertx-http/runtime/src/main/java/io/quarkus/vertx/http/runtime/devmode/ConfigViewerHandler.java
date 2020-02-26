package io.quarkus.vertx.http.runtime.devmode;

import javax.enterprise.inject.spi.CDI;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ConfigViewerHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        ConfigHolder configHolder = CDI.current().select(ConfigHolder.class).get();
        JsonObject json = new ConfigViewer().dump(configHolder.config);
        HttpServerResponse resp = routingContext.response();
        resp.putHeader("content-type", "application/json");
        resp.end(Buffer.buffer(json.encode()));
    }
}
