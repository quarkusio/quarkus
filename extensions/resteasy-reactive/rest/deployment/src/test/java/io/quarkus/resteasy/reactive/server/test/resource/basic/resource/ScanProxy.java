package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/test")
public interface ScanProxy {
    @Path("/subrsource")
    ScanSubresource doit();

    @Path("/doit")
    @GET
    @Produces("text/plain")
    String get();
}
