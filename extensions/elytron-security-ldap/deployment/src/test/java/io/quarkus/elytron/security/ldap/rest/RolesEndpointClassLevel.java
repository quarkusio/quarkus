package io.quarkus.elytron.security.ldap.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * Test JAXRS endpoint with RolesAllowed specified at the class level
 */
@Path("/roles-class")
@RolesAllowed("standardRole")
public class RolesEndpointClassLevel {
    @GET
    public String echo(@Context SecurityContext sec) {
        return "Hello " + sec.getUserPrincipal().getName();
    }

}
