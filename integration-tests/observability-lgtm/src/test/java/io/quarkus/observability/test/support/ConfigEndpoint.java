package io.quarkus.observability.test.support;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.config.Config;

@Path("/config")
public class ConfigEndpoint {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/grafana")
    public String grafana() {
        return Config.get().getValue("grafana.endpoint", String.class);
    }
}
