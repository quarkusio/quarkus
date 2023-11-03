package io.quarkus.it.smallrye.config;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

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
    @Path("/host")
    public Response getServerHost() throws Exception {
        Method method = server.getClass().getDeclaredMethod("host");
        return Response.ok(method.invoke(server)).build();
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
