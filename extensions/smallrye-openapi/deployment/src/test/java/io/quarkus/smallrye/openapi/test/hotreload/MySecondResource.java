package io.quarkus.smallrye.openapi.test.hotreload;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/my-second-api")
public class MySecondResource {

    @GET
    public String hello() {
        return "hello";
    }

}
