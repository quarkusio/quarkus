package io.quarkus.it.rest;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
    @Path("callingAuthenticated")
    public String callingAuthenticated() {
        return bean.authenticated();
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
