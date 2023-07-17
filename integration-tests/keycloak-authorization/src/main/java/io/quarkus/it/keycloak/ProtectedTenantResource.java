package io.quarkus.it.keycloak;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/api-permission-tenant")
public class ProtectedTenantResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public Uni<List<Permission>> permissions() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }
}
