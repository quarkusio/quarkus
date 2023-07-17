package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.smallrye.common.annotation.Blocking;

@Path("/roles-validate")
@PermitAll
@Blocking
public class SerializationRolesResource {
    @POST
    @RolesAllowed({ "user", "admin" })
    public String defaultSecurity(SerializationEntity entity) {
        return entity.getName();
    }

    @Path("/admin")
    @RolesAllowed("admin")
    @POST
    public String admin(SerializationEntity entity) {
        return entity.getName();
    }

}
