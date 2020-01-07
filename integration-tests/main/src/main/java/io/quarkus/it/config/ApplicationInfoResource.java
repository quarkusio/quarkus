package io.quarkus.it.config;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/application-info")
public class ApplicationInfoResource {

    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return applicationName + "/" + applicationVersion;
    }
}
