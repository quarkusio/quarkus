package io.quarkus.it.external.exporter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

@Path("hello")
@Produces(MediaType.APPLICATION_JSON)
public class HelloResource {
    private static final Logger LOG = LoggerFactory.getLogger(HelloResource.class);
    public static final String HISTOGRAM_NAME = "example_histogram";

    @Inject
    Meter meter;

    @GET
    public String get() {
        LongHistogram histogram = meter.histogramBuilder(HISTOGRAM_NAME).ofLongs().build();
        histogram.record(10, Attributes.of(AttributeKey.stringKey("key"), "value"));
        LOG.info("Hello World");
        return "get";
    }
}
