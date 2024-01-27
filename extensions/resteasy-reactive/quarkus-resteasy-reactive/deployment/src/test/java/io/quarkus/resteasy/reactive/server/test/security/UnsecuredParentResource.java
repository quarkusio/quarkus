package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class UnsecuredParentResource {

    @Path("/defaultSecurityParent")
    @GET
    public String defaultSecurityParent() {
        return "defaultSecurityParent";
    }

}
