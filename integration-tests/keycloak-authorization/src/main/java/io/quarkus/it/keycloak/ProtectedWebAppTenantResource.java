package io.quarkus.it.keycloak;

import java.util.List;

import javax.inject.Inject;
import javax.security.auth.AuthPermission;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/api-permission-webapp")
@Authenticated
public class ProtectedWebAppTenantResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public Uni<List<Permission>> permissions() {
        return identity.checkPermission(new AuthPermission("Permission Resource WebApp")).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }
}
