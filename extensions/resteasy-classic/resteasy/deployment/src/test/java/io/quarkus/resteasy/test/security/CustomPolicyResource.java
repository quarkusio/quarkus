package io.quarkus.resteasy.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("custom-policy")
public class CustomPolicyResource {

    @Path("is-admin")
    @GET
    public boolean isUserAdmin(@Context SecurityContext context) {
        return context.isUserInRole("admin");
    }

}
