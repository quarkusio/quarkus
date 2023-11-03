package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/hello")
public class ExampleResource {

    @ConfigProperty(name = "my-app-name")
    String appName;
    @ConfigProperty(name = "quarkus.application.version")
    String appVersion;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return appName + " " + appVersion;
    }
}
