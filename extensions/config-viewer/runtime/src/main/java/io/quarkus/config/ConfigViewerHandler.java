package io.quarkus.config;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.eclipse.microprofile.config.Config;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class ConfigViewerHandler implements Handler<RoutingContext> {

    private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);

    @Override
    public void handle(RoutingContext routingContext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Config config = CDI.current().select(Config.class).get();

        JsonBuilderFactory builderFactory = Json.createBuilderFactory(JSON_CONFIG);
        JsonWriterFactory factory = Json.createWriterFactory(JSON_CONFIG);
        JsonWriter writer = factory.createWriter(out);
        JsonObject json = new ConfigViewer().dump(config, builderFactory);
        writer.writeObject(json);
        writer.close();

        HttpServerResponse resp = routingContext.response();
        resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        resp.end(Buffer.buffer(out.toByteArray()));
    }
}
