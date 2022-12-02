package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/test")
public interface ScanProxy {
    @Path("/subrsource")
    ScanSubresource doit();

    @Path("/doit")
    @GET
    @Produces("text/plain")
    String get();
}
