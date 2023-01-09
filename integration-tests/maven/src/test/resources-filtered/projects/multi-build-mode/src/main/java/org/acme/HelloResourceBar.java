package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@IfBuildProfile("bar")
@Path("/hello")
public class HelloResourceBar {

    @Inject
    HelloService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Bar: hello, " + service.name() + "-" + service.classFounds();
    }
}
