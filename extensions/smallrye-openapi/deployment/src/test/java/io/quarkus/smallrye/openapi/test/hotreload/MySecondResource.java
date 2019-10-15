package io.quarkus.smallrye.openapi.test.hotreload;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/my-second-api")
public class MySecondResource {

    @GET
    public String hello() {
        return "hello";
    }

}
