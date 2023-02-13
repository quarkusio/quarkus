package io.quarkus.it.opentracing;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {

    @Inject
    TracedService tracedService;

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
