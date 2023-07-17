package io.quarkus.elytron.security.ldap.rest;

import java.security.Principal;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("subject")
public class SubjectExposingResource {

    @Inject
    Principal principal;

    @GET
    @RolesAllowed("standardRole")
    @Path("secured")
    public String getSubjectSecured(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
        return name;
    }

    @GET
    @RolesAllowed("standardRole")
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
