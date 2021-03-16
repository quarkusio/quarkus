package io.quarkus.it.opentracing;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.jaegertracing.internal.JaegerTracer;

@Path("/opentracing")
public class OpenTracingResource {
    @Inject
    TracedService tracedService;

    // calling this endpoint should generate a trace with two spans
    @GET
    public String getTest() {
        return tracedService.testTraced();
    }

    @GET
    @Path("/jaegerversion")
    @Produces(MediaType.TEXT_PLAIN)
    public String opentracingVersion() {
        return JaegerTracer.getVersionFromProperties();
    }

}
