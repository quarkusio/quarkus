package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "my-custom-text-file.txt is on the classpath: "
            + (Thread.currentThread().getContextClassLoader().getResource("my-custom-text-file.txt") != null);
    }
}
