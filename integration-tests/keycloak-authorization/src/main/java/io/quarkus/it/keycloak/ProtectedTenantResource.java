package io.quarkus.it.keycloak;

import java.util.List;

import javax.inject.Inject;
import javax.security.auth.AuthPermission;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/api-permission-tenant")
public class ProtectedTenantResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public Uni<List<Permission>> permissions() {
        return identity.checkPermission(new AuthPermission("Permission Resource Tenant")).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }
}
