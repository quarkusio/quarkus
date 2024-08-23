package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("{resource.path}")
public class {resource.class-name} {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "{resource.response}";
    }
}
