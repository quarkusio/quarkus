package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
