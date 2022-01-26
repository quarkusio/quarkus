package org.acme.rest;

import org.acme.level0.Level0Service;
import org.acme.level2.submodule.Level2Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
