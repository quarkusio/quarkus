package io.quarkus.rest.client.reactive.timeout;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public interface Client {
    @GET
    @Path("/slow")
    String slow();

    @GET
    @Path("/fast")
    String fast();
}
