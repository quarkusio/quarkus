package io.quarkus.smallrye.openapi.test.hotreload;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api")
public class MyResource {

    @GET
    public String hello() {
        return "hello";
    }

    // <placeholder>

}
