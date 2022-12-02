package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@IfBuildProfile("bar-2")
@Path("/hello")
public class HelloResourceBar {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello bar 2";
    }
}
