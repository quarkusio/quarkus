package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@IfBuildProfile("foo")
@Path("/hello")
public class HelloResourceFoo {

    @Inject
    HelloService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Foo: hello, " + service.name() + "-" + service.classFounds();
    }
}
