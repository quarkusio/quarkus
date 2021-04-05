package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
