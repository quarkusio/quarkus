package io.quarkus.it.opentracing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.opentracing.Traced;

import io.jaegertracing.internal.JaegerTracer;

@Path("/opentracing")
public class OpenTracingResource {

    @GET
    @Traced
    public String getTest() {
        return "TEST";
    }

    @GET
    @Path("/jaegerversion")
    @Produces(MediaType.TEXT_PLAIN)
    public String opentracingVersion() {
        return JaegerTracer.getVersionFromProperties();
    }

}
