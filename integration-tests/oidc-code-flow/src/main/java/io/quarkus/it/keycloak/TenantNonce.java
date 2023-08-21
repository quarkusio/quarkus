package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-nonce")
public class TenantNonce {

    @Inject
    OidcSession session;
    @Inject
    RoutingContext routingContext;

    @GET
    @Authenticated
    public String getTenant() {
        return session.getTenantId() + (routingContext.get("reauthenticated") != null ? ":reauthenticated" : "");
    }
}
