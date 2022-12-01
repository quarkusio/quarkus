package io.quarkus.it.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.it.Main;

@Path("/greeting")
public class GreetingEndpoint {

    @Inject
    GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{name}")
    public String greet(@PathParam String name) {
        String[] params = Main.PARAMS;
        if (params != null && params.length > 0) {
            return params[0] + " " + name;
        }
        return greetingService.greet(name);
    }
}
