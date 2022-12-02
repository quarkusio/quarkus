package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
