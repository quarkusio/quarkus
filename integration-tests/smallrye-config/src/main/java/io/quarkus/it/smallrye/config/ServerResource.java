package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/server")
public class ServerResource {
    @Inject
    Server server;

    @GET
    public Response getServer() {
        return Response.ok(server).build();
    }
}
