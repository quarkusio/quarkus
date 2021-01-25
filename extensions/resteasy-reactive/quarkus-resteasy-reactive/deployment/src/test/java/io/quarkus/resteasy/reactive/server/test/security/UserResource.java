package io.quarkus.resteasy.reactive.server.test.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import io.smallrye.common.annotation.Blocking;

@Path("/user")
@Blocking
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
