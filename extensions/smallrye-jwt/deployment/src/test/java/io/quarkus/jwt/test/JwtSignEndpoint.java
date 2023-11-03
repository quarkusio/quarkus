package io.quarkus.jwt.test;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("jwtsign")
public class JwtSignEndpoint {
    @GET
    @RolesAllowed("admin")
    public Response get() {
        return Response.ok("success").build();
    }
}
