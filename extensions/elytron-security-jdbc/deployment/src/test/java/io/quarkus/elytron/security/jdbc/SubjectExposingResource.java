package io.quarkus.elytron.security.jdbc;

import java.security.Principal;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@Path("subject")
public class SubjectExposingResource {

    @Inject
    Principal principal;

    @GET
    @RolesAllowed("user")
    @Path("secured")
    public String getSubjectSecured(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
        return name;
    }

    @GET
    @RolesAllowed("user")
    @Path("principal-secured")
    public String getPrincipalSecured(@Context SecurityContext sec) {
        if (principal == null) {
            throw new IllegalStateException("No injected principal");
        }
        String name = principal.getName();
        return name;
    }

    @GET
    @Path("unsecured")
    @PermitAll
    public String getSubjectUnsecured(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
        return name;
    }

    @DenyAll
    @GET
    @Path("denied")
    public String getSubjectDenied(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
        return name;
    }
}
