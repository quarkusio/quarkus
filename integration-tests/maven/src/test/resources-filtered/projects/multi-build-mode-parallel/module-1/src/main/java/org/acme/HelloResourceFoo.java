package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@IfBuildProfile("foo-1")
@Path("/hello")
public class HelloResourceFoo {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello foo 1";
    }
}
