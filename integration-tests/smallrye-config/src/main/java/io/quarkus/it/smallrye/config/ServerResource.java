package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@Path("/server")
public class ServerResource {
    @Inject
    Server server;
    @Inject
    @ConfigProperties
    ServerProperties serverProperties;

    @GET
    public Response getServer() {
        return Response.ok(server).build();
    }

    @GET
    @Path("/properties")
    public Response getServerProperties() {
        return Response.ok(serverProperties).build();
    }
}
