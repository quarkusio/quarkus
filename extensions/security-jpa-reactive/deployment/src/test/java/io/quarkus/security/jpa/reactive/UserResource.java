package io.quarkus.security.jpa.reactive;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@RolesAllowed("user")
@Path("/user")
public class UserResource {

    @WithTransaction
    @POST
    public Uni<Response> createUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Invalid username");
        }

        // create new user with role 'user' and do not validate whether username is unique
        return PanacheRoleEntity.<PanacheRoleEntity> find("role", "user").singleResult().flatMap(userRole -> {
            PanacheUserEntity user = new PanacheUserEntity();
            user.name = username;
            user.pass = username;
            user.roles = List.of(userRole);
            return user.persist();
        }).map(user -> Response.created(null).build());
    }

    @GET
    public Uni<String> getUsername(@Context SecurityContext sec) {
        var principal = requireNonNull(sec.getUserPrincipal());
        return Uni.createFrom().item(principal.getName());
    }

}
