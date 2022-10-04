package io.quarkus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

@Path("/server-requests")
@ApplicationScoped
public class ExporterResource {
    @Inject
    MeterRegistry registry;

    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Integer countServerRequests(@QueryParam("method") String method,
            @QueryParam("outcome") String outcome,
            @QueryParam("status") String status,
            @QueryParam("uri") String uri) {
        final Search search = registry
                .find("http.server.requests");
        if (method != null) {
            search.tag("method", method);
        }
        if (outcome != null) {
            search.tag("outcome", outcome);
        }
        if (status != null) {
            search.tag("status", status);
        }
        if (uri != null) {
            search.tag("uri", uri);
        }
        return search.timers().size();
    }
}
