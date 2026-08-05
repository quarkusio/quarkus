package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public abstract class SimpleBaseResource {

    @GET
    @Path("/simple")
    public String simpleMethod() {
        return "simple";
    }
}
