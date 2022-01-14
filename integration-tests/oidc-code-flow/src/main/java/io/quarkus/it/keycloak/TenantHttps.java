package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-https")
@Authenticated
public class TenantHttps {

    @Inject
    OidcSession session;
    @Inject
    RoutingContext routingContext;

    @GET
    public String getTenant() {
        return session.getTenantId() + (routingContext.get("reauthenticated") != null ? ":reauthenticated" : "");
    }

    @GET
    @Path("query")
    public String getTenantWithQuery(@QueryParam("a") String value) {
        return getTenant() + "?a=" + value;
    }
}
