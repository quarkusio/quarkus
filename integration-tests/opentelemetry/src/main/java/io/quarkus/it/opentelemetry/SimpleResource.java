package io.quarkus.it.opentelemetry;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {
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
        TraceData data = new TraceData();
        data.message = "Direct trace";

        return data;
    }

    @GET
    @Path("/chained")
    public TraceData chainedTrace() {
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
}
