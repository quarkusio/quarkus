package io.quarkus.it.opentelemetry.vertx.exporter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;

@Path("hello")
public class HelloResource {
    private static final Logger LOG = LoggerFactory.getLogger(HelloResource.class);
    @Inject
    Meter meter;

    @GET
    public String get() {
        meter.counterBuilder("hello").build().add(1, Attributes.of(AttributeKey.stringKey("key"), "value"));
        LOG.info("Hello World");
        return "get";
    }

}
