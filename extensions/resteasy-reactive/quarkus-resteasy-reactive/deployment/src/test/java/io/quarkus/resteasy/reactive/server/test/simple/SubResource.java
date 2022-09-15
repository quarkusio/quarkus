package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class SubResource {

    @GET
    public String sub() {
        return "sub";
    }

    @GET
    @Path("otherSub")
    public String otherPath() {
        return "otherSub";
    }
}
