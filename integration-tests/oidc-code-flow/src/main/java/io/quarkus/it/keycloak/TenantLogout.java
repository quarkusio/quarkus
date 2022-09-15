package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;

import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-logout")
public class TenantLogout {

    @Context
    HttpHeaders headers;

    @Inject
    RoutingContext context;

    @Authenticated
    @GET
    public String getTenantLogout() {
        return "Tenant Logout, refreshed: " + (context.get("refresh_token_grant_response") != null);
    }

    // It is needed for the proactive-auth=false to work: /tenant-logout/logout should match a user initiated logout request
    // which must be handled by `CodeAuthenticationMechanism`.
    // Adding `@Authenticated` gives control to `CodeAuthenticationMechanism` instead of RestEasy.
    @GET
    @Authenticated
    @Path("logout")
    public String getTenantLogoutPath() {
        throw new InternalServerErrorException();
    }

    @GET
    @Path("post-logout")
    public String postLogout(@QueryParam("state") String postLogoutState) {
        Cookie cookie = headers.getCookies().get("q_post_logout_tenant-logout");
        if (cookie == null) {
            throw new InternalServerErrorException("q_post_logout cookie is not available");
        }
        if (postLogoutState == null) {
            throw new InternalServerErrorException("'state' query parameter is not available");
        }
        if (!postLogoutState.equals(cookie.getValue())) {
            throw new InternalServerErrorException("'state' query parameter is not equal to the q_post_logout cookie value");
        }
        return "You were logged out";
    }
}
