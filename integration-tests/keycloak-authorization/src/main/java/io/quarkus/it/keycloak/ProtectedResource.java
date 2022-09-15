package io.quarkus.it.keycloak;

import java.security.BasicPermission;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.AuthPermission;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

@Path("/api/permission")
public class ProtectedResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    AuthzClient authzClient;

    @GET
    @Path("/tenant")
    public Uni<List<Permission>> permissionsTenant() {
        return permissions();
    }

    @GET
    public Uni<List<Permission>> permissions() {
        return identity.checkPermission(new AuthPermission("Permission Resource")).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }

    @GET
    @Path("/scope")
    public Uni<List<Permission>> hasScopePermission(@QueryParam("scope") String scope) {
        return identity.checkPermission(new BasicPermission("Scope Permission Resource") {
            @Override
            public String getActions() {
                return scope;
            }
        }).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }

    @Path("/claim-protected")
    @GET
    public List<Permission> claimProtected() {
        return identity.getAttribute("permissions");
    }

    @Path("/http-response-claim-protected")
    @GET
    public List<Permission> httpResponseClaimProtected() {
        return identity.getAttribute("permissions");
    }

    @Path("/body-claim")
    @POST
    public List<Permission> bodyClaim(Map<String, Object> body, @Context HttpServerRequest request) {
        if (body == null || !body.containsKey("from-body")) {
            return Collections.emptyList();
        }
        return identity.getAttribute("permissions");
    }

    @Path("/entitlements")
    @GET
    public String getEntitlements() {
        return authzClient.authorization().authorize().getToken();
    }

    @Produces("application/json")
    @Path("/party-token-permissions-size")
    @GET
    public int getPartyTokenPermissionsSize() {
        return authzClient.protection().introspectRequestingPartyToken(authzClient.authorization().authorize().getToken())
                .getPermissions().size();
    }
}
