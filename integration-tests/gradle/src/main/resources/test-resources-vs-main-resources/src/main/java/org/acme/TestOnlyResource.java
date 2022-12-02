package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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