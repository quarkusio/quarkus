package org.acme.quarkus.sample;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        String foo = System.getenv("FOO_VALUE"); // abc
        String bar = System.getenv("BAR_VALUE"); // def
        return foo + bar; // abcdef
    }
}
