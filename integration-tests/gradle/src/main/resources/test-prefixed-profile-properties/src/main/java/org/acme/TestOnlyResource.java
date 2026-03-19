package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/test-only")
public class TestOnlyResource {

    @ConfigProperty(name = "test-only")
    String testOnly;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return testOnly;
    }
}
