package io.quarkus.it.smallrye.config;

import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;

@Path("/server")
public class ServerResource {
    @Inject
    Config config;
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
    @Path("/validator/cloud")
    public Response validator() {
        try {
            config.unwrap(SmallRyeConfig.class).getConfigMapping(Cloud.class, "cloud");
        } catch (ConfigValidationException e) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for (int i = 0; i < e.getProblemCount(); i++) {
                jsonArrayBuilder.add(e.getProblem(i).getMessage());
            }
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("errors", jsonArrayBuilder);
            return Response.ok().entity(jsonObjectBuilder.build().toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.serverError().build();
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
