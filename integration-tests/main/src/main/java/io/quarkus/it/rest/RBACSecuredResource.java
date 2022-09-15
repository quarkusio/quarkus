package io.quarkus.it.rest;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/rbac-secured")
public class RBACSecuredResource {

    @Inject
    RBACBean bean;

    @GET
    @RolesAllowed("tester")
    @Path("forTesterOnly")
    public String forTesterOnly() {
        return "forTesterOnly";
    }

    @GET
    @RolesAllowed("tester")
    @Path("forTesterOnlyWithMethodParamAnnotations")
    public String forTesterOnlyWithMethodParamAnnotations(@Context SecurityContext ctx, @Context UriInfo uriInfo,
            @Valid String message) {
        return "forTesterOnlyWithMethodParamAnnotations";
    }

    @GET
    @DenyAll
    @Path("denied")
    public String denied() {
        return "denied";
    }

    @GET
    @PermitAll
    @Path("permitted")
    public String permitted() {
        return "permitted";
    }

    @GET
    @Authenticated
    @Path("authenticated")
    public String authenticated() {
        return "authenticated";
    }

    @GET
    @RolesAllowed("**")
    @Path("allRoles")
    public String allRoles() {
        return "allRoles";
    }

    @GET
    @Path("callingAuthenticated")
    public String callingAuthenticated() {
        return bean.authenticated();
    }

    @GET
    @Path("callingAllRoles")
    public String callingAllRoles() {
        return bean.allRoles();
    }

    @GET
    @Path("callingPermitted")
    public String callingPermitted() {
        return bean.permitted();
    }

    @GET
    @Path("callingTesterOnly")
    public String callingTesterOnly() {
        return bean.testerOnly();
    }

    @GET
    @Path("callingDenied")
    public String callingDenied() {
        return bean.denied();
    }

}
