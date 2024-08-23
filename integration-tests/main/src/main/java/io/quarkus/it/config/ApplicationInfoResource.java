package io.quarkus.it.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/application-info")
public class ApplicationInfoResource {

    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @ConfigProperty(name = "quarkus.profile")
    String applicationProfile;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return applicationName + "/" + applicationVersion + "/" + applicationProfile;
    }
}
