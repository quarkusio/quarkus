package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.Authenticated;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-refresh")
public class TenantRefresh {
    @Inject
    RoutingContext context;

    @Inject
    TenantConfigBean tenantConfig;

    @Authenticated
    @GET
    public String getTenantRefresh() {
        return "Tenant Refresh, refreshed: " + (context.get("refresh_token_grant_response") != null);
    }

    @GET
    @Path("/session-expired-page")
    public String sessionExpired(@CookieParam("session_expired") String sessionExpired,
            @QueryParam("session-expired") boolean expired, @QueryParam("redirect-filtered") String filtered)
            throws Exception {
        if (expired && filtered.equals("true,")) {
            // Cookie format: jwt|<tenant id>

            String[] pair = sessionExpired.split("\\|");
            OidcTenantConfig oidcConfig = tenantConfig.getStaticTenant(pair[1]).getOidcTenantConfig();
            JsonWebToken jwt = new DefaultJWTParser().decrypt(pair[0], oidcConfig.credentials.secret.get());

            OidcUtils.removeCookie(context, oidcConfig, "session_expired");

            return jwt.getClaim(Claims.preferred_username) + ", your session has expired. "
                    + "Please login again at http://localhost:8081/" + oidcConfig.tenantId.get();
        }

        throw new RuntimeException("Invalid session expired page redirect");
    }
}
