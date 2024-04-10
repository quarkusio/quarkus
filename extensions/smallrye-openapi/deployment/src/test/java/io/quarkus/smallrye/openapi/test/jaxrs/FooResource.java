package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.core.Response;

@RolesAllowed("RoleXY")
public class FooResource implements FooAPI {
    @Override
    public Response getFoo() {
        return Response.ok("ok").build();
    }
}
