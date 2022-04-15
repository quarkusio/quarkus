package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-https")
public class TenantHttps {

    @Inject
    OidcSession session;
    @Inject
    RoutingContext routingContext;

    @GET
    @Authenticated
    public String getTenant() {
        return session.getTenantId() + (routingContext.get("reauthenticated") != null ? ":reauthenticated" : "");
    }

    @GET
    @Path("query")
    @Authenticated
    public String getTenantWithQuery(@QueryParam("code") String value) {
        return getTenant() + "?code=" + value;
    }

    @GET
    @Path("error")
    public String getError(@QueryParam("error") String error, @QueryParam("error_description") String errorDescription) {
        return "error: " + error + ", error_description: " + errorDescription;
    }
}
