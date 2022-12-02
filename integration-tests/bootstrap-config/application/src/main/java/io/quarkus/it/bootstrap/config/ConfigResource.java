package io.quarkus.it.bootstrap.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
