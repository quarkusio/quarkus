package io.quarkus.resteasy.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public interface UnsecuredResourceInterface {

    @Path("/defaultSecurityInterface")
    @GET
    default String defaultSecurityInterface() {
        return "defaultSecurityInterface";
    }

}
