package org.acme.rest;

import org.acme.level0.Level0Service;
import org.acme.level2.submodule.Level2Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/app")
public class HelloResource {

    @Inject
    Level0Service level0Service;

    @Inject
    Level2Service level2Service;

    // for manual test
    @GET
    @Path("/hello-0")
    @Produces(MediaType.TEXT_PLAIN)
    public String getGreetingFromLevel0() {
        return level0Service.getGreeting();
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String getGreetingFromLevel2() {
        return level2Service.getGreetingFromLevel1();
    }
}
