package io.quarkus.security.test;

import java.security.Principal;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 8/8/18
 */
@Path("subject")
public class SubjectExposingResource {

    @GET
    @RolesAllowed("Tester")
    @Path("secured")
    public String getSubjectSecured(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
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