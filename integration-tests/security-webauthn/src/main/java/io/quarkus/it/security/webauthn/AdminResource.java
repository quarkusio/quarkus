package io.quarkus.it.security.webauthn;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/api/admin")
public class AdminResource {

    @Context
    SecurityIdentity identity;

    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    public String adminResource() {
        return "admin";
    }

    public record Dto(String payload) {
    }

    @Path("/payload")
    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> adminResourceWithPayload(Dto dto) {
        String response = dto.payload + " - " + identity.getPrincipal().getName();
        return Uni.createFrom().item(response);
    }
}
