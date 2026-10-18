package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path("/greeting")
public class GreetingEndpoint {

    @Inject
    GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{name}")
    public String greet(@PathParam("name") String name) {
        String[] params = Main.PARAMS;
        if (params != null && params.length > 0) {
            return params[0] + " " + name;
        }
        return greetingService.greet(name);
    }
}
