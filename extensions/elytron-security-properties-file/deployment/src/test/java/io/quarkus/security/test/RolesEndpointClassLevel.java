package io.quarkus.security.test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * Test JAXRS endpoint with RolesAllowed specified at the class level
 */
@Path("/rolesClass")
@RolesAllowed("Tester")
public class RolesEndpointClassLevel {
    @GET
    public String echo(@Context SecurityContext sec) {
        return "Hello " + sec.getUserPrincipal().getName();
    }

}
