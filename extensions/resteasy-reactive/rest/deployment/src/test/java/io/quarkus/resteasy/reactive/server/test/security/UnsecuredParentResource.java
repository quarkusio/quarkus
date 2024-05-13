package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class UnsecuredParentResource {

    @Path("/defaultSecurityParent")
    @GET
    public String defaultSecurityParent() {
        return "defaultSecurityParent";
    }

    @RolesAllowed({ "admin", "user" })
    @GET
    @Path("/parent-annotated")
    public String parentAnnotated() {
        return "parent-annotated";
    }

}
