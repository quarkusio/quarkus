package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

public class ParameterSubResSubImpl2 {

    @GET
    @Produces("text/plain")
    public String get() {
        return "Boo2";
    }

    @Path("/unrelated")
    @Produces("text/plain")
    public String noHttpMethod() {
        return "Whatever - this method exists just to simulate 49172";
    }
}
