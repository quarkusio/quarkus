package io.quarkus.security.test;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

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
