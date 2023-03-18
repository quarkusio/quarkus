package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;

@Path("/permissions-non-blocking")
@PermitAll
public class NonBlockingPermissionsAllowedResource {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @POST
    @PermissionsAllowed("create")
    @PermissionsAllowed("update")
    public Uni<String> createOrUpdate() {
        return Uni.createFrom().item("done");
    }

    @Path("/admin")
    @PermissionsAllowed({ "read:resource-admin", "read:resource-user" })
    @GET
    public Uni<String> admin() {
        return Uni.createFrom().item("admin");
    }

    @Path("/admin/security-identity")
    @PermissionsAllowed("get-identity")
    @GET
    public Uni<String> getSecurityIdentity() {
        return Uni.createFrom().item(currentIdentityAssociation.getIdentity().getPrincipal().getName());
    }

    @PermissionsAllowed(value = "perm1", permission = CustomPermission.class)
    @Path("/custom-permission")
    @GET
    public Uni<String> greetings(@QueryParam("greeting") String greeting) {
        return Uni.createFrom().item(greeting);
    }
}
