package io.quarkus.resteasy.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


public class UnsecuredSubResource {
    @GET
    @Path("/subMethod")
    public String subMethod() {
        return "subMethod";
    }
}
