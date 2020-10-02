package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import io.quarkus.security.Authenticated;

@Path("/tenant-logout")
public class TenantLogout {

    @Context
    HttpHeaders headers;

    @Authenticated
    @GET
    public String getTenantLogout() {
        return "Tenant Logout";
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
