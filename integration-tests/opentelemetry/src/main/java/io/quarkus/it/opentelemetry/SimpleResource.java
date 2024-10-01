package io.quarkus.it.opentelemetry;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Scope;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleResource.class);

    @RegisterRestClient(configKey = "simple")
    public interface SimpleClient {
        @Path("")
        @GET
        TraceData noPath();

        @Path("/")
        @GET
        TraceData slashPath();

        @Path("/from-baggage")
        @GET
        TraceData fromBaggagePath();
    }

    @Inject
    TracedService tracedService;

    @Inject
    @RestClient
    SimpleClient simpleClient;

    @Inject
    Baggage baggage;

    @Inject
    Meter meter;

    @GET
    public TraceData noPath() {
        TraceData data = new TraceData();
        data.message = "No path trace";
        return data;
    }

    @GET
    @Path("/nopath")
    public TraceData noPathClient() {
        return simpleClient.noPath();
    }

    @GET
    @Path("/slashpath")
    public TraceData slashPathClient() {
        return simpleClient.slashPath();
    }

    @GET
    @Path("/slashpath-baggage")
    public TraceData slashPathBaggageClient() {
        try (Scope scope = baggage.toBuilder()
                .put("baggage-key", "baggage-value")
                .build()
                .makeCurrent()) {
            return simpleClient.fromBaggagePath();
        }
    }

    @GET
    @Path("/from-baggage")
    public TraceData fromBaggageValue() {
        TraceData data = new TraceData();
        data.message = baggage.getEntryValue("baggage-key");
        return data;
    }

    @GET
    @Path("/direct")
    public TraceData directTrace() {
        LOG.info("directTrace called");
        TraceData data = new TraceData();
        data.message = "Direct trace";
        return data;
    }

    @GET
    @Path("/direct-metrics")
    public TraceData directTraceWithMetrics() {
        meter.counterBuilder("direct-trace-counter")
                .setUnit("items")
                .setDescription("A counter of direct traces")
                .build()
                .add(1, Attributes.of(AttributeKey.stringKey("key"), "low-cardinality-value"));
        TraceData data = new TraceData();
        data.message = "Direct trace";
        return data;
    }

    @GET
    @Path("/chained")
    public TraceData chainedTrace() {
        LOG.info("chainedTrace called");
        TraceData data = new TraceData();
        data.message = tracedService.call();

        return data;
    }

    @GET
    @Path("/deep/path")
    public TraceData deepUrlPathTrace() {
        TraceData data = new TraceData();
        data.message = "Deep url path";

        return data;
    }

    @GET
    @Path("/param/{paramId}")
    public TraceData pathParameters(@PathParam("paramId") String paramId) {
        TraceData data = new TraceData();
        data.message = "ParameterId: " + paramId;

        return data;
    }

    @GET
    @Path("/exception")
    public String exception() {
        var exception = new RuntimeException("Exception!");
        StackTraceElement[] trimmedStackTrace = new StackTraceElement[2];
        System.arraycopy(exception.getStackTrace(), 0, trimmedStackTrace, 0, trimmedStackTrace.length);
        exception.setStackTrace(trimmedStackTrace);
        LOG.error("Oh no {}", exception.getMessage(), exception);
        return "Oh no! An exception";
    }
}
