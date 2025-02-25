package io.quarkus.it.smallrye.config;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/server")
public class ServerResource {
    @Inject
    Server server;
    @Inject
    @ConfigProperties
    ServerProperties serverProperties;
    @Inject
    @ConfigProperty(name = "server.info.message")
    Instance<String> message;
    @Inject
    @ConfigProperty(name = "http.server.form.positions")
    List<Integer> positions;

    @GET
    public Response getServer() {
        return Response.ok(server).build();
    }

    @GET
    @Path("/properties")
    public Response getServerProperties() {
        return Response.ok(serverProperties).build();
    }

    @GET
    @Path("/info")
    public String info() {
        return message.get();
    }

    @GET
    @Path("/positions")
    public List<Integer> positions() {
        return positions;
    }
}
