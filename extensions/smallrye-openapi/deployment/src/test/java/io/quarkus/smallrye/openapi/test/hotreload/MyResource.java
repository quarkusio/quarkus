package io.quarkus.smallrye.openapi.test.hotreload;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api")
public class MyResource {

    @GET
    public String hello() {
        return "hello";
    }

    // <placeholder>

}
