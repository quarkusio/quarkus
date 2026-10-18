package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
public class HelloResource {
    @GET
    public String hello() {
        return "hello";
    }

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @GET
    @Path("/applicationName")
    public String applicationName() {
        return applicationName;
    }
}
