package io.quarkus.resteasy.test.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@Path("/user")
public class UserResource {

    @Context
    SecurityContext securityContext;

    @GET
    public String get() {
        if (securityContext.getUserPrincipal() == null) {
            return null;
        }
        return securityContext.getUserPrincipal().getName();
    }

}
