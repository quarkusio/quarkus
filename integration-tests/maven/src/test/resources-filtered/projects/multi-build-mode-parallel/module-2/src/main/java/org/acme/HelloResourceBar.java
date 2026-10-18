package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@IfBuildProfile("bar-2")
@Path("/hello")
public class HelloResourceBar {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello bar 2";
    }
}
