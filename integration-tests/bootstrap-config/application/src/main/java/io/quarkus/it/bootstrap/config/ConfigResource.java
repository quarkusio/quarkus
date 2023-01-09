package io.quarkus.it.bootstrap.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.config.SmallRyeConfig;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {
    @Inject
    SmallRyeConfig config;

    @GET
    @Path("/{name}")
    public Response configValue(@PathParam("name") final String name) {
        return Response.ok(config.getConfigValue(name)).build();
    }

    @RegisterForReflection(targets = {
            org.eclipse.microprofile.config.ConfigValue.class,
            io.smallrye.config.ConfigValue.class
    })
    public static class ConfigValueReflection {

    }
}
