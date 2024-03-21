package io.quarkus.it.opentelemetry.vertx.exporter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;

@Path("hello")
public class HelloResource {

    @Inject
    Meter meter;

    @GET
    public String get() {
        meter.counterBuilder("hello").build().add(1, Attributes.of(AttributeKey.stringKey("key"), "value"));
        return "get";
    }

}
