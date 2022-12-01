package org.acme;


import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/app-config")
public class ApplicationConfigResource {

    @ConfigProperty(name = "quarkus.application.name")
    String name;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return String.format("%s:%s", name, version);
    }
}
