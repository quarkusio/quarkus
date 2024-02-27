package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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