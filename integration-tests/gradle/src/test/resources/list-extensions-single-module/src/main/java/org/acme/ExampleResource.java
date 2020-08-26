package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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