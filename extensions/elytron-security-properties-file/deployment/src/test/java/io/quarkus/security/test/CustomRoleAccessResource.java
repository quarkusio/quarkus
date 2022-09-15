package io.quarkus.security.test;

import java.security.Principal;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("custom-role")
public class CustomRoleAccessResource {

    @Inject
    Principal principal;

    @GET
    @RolesAllowed("custom")
    public String getSubjectSecured(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }
}
