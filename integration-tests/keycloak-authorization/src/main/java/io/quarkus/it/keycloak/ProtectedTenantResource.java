package io.quarkus.it.keycloak;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("")
public class ProtectedTenantResource {

    @Inject
    SecurityIdentity identity;

    @Path("api-permission-tenant")
    @GET
    public Uni<List<Permission>> apiPermissions() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @Path("dynamic-permission-tenant")
    @GET
    public Uni<List<Permission>> dynamicPermissions() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }
}
