package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/server")
@Produces(MediaType.APPLICATION_JSON)
public class ServerResource {
    @Inject
    Server server;

    @GET
    public Response getServer() {
        return Response.ok(server).build();
    }
}
