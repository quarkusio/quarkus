package io.quarkus.smallrye.openapi.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/resource")
public class OpenApiResource {

    @GET
    public String root() {
        return "resource";
    }
}
