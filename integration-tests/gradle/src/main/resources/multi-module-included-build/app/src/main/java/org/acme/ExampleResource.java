package org.acme;

import org.acme.lib.LibService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @Inject
    LibService libService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "foo " + libService.bar();
    }
}
