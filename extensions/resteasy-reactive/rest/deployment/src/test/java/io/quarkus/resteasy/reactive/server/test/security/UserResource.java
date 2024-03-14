package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

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
