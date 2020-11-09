package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Path("/web-app")
@Authenticated
public class ProtectedResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    IdTokenCredential idTokenCredential;

    @Inject
    JsonWebToken accessToken;

    @Inject
    AccessTokenCredential accessTokenCredential;

    @Inject
    RefreshToken refreshToken;

    @Context
    SecurityContext securityContext;

    @GET
    @Path("sec")
    public String hello() {
        return securityContext.getUserPrincipal().getName();
    }

    @GET
    public String getName() {
        if (!idTokenCredential.getToken().equals(idToken.getRawToken())) {
            throw new OIDCException("ID token values are not equal");
        }
        if (idTokenCredential.getRoutingContext() != identity.getAttribute(RoutingContext.class.getName())) {
            throw new OIDCException("SecurityIdentity must have a RoutingContext attribute");
        }
        return idToken.getName();
    }

    @GET
    @Path("tenant-idtoken-only")
    public String getNameIdTokenOnly() {
        return "tenant-idtoken-only:" + getName();
    }

    @GET
    @Path("tenant-split-tokens")
    public String getNameSplitTokens() {
        return "tenant-split-tokens:" + getName();
    }

    @GET
    @Path("callback-before-wrong-redirect")
    public String getNameCallbackBeforeWrongRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }

    @GET
    @Path("callback-before-redirect")
    public String getNameCallbackBeforeRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }

    @GET
    @Path("callback-after-redirect")
    public String getNameCallbackAfterRedirect() {
        return "callback:" + getName();
    }

    @GET
    @Path("callback-jwt-before-redirect")
    public String getNameCallbackJwtBeforeRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }

    @GET
    @Path("callback-jwt-after-redirect")
    public String getNameCallbackJwtAfterRedirect() {
        return "callback-jwt:" + getName();
    }

    @GET
    @Path("callback-jwt-not-used-before-redirect")
    public String getNameCallbackJwtNotUsedBeforeRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }

    @GET
    @Path("callback-jwt-not-used-after-redirect")
    public String getNameCallbackJwtNotUsedAfterRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }

    @GET
    @Path("tenant-logout")
    public String getTenantLogout() {
        return "Tenant Logout";
    }

    @GET
    @Path("access")
    public String getAccessToken() {
        if (accessToken.getRawToken() != null && !accessTokenCredential.getToken().equals(accessToken.getRawToken())) {
            throw new OIDCException("Access token values are not equal");
        }
        return accessToken.getRawToken() != null && !accessToken.getRawToken().isEmpty() ? "AT injected" : "no access";
    }

    @GET
    @Path("access/tenant-idtoken-only")
    public String getAccessTokenIdTokenOnly() {
        return "tenant-idtoken-only:" + getAccessToken();
    }

    @GET
    @Path("access/tenant-split-tokens")
    public String getAccessTokenSplitTokens() {
        return "tenant-split-tokens:" + getAccessToken();
    }

    @GET
    @Path("refresh")
    public String getRefreshToken() {
        if (refreshToken.getToken() != null
                && !accessTokenCredential.getRefreshToken().getToken().equals(refreshToken.getToken())) {
            throw new OIDCException("Refresh token values are not equal");
        }
        if (refreshToken.getToken() != null && !refreshToken.getToken().isEmpty()) {
            String message = "RT injected";
            String listenerMessage = idTokenCredential.getRoutingContext().get("listener-message");
            if (listenerMessage != null) {
                message += ("(" + listenerMessage + ")");
            }
            return message;
        } else {
            return "no refresh";
        }
    }

    @GET
    @Path("refresh/tenant-idtoken-only")
    public String getRefreshTokenIdTokenOnly() {
        return "tenant-idtoken-only:" + getRefreshToken();
    }

    @GET
    @Path("refresh/tenant-split-tokens")
    public String getRefreshTokenSplitTokens() {
        return "tenant-split-tokens:" + getRefreshToken();
    }

    @GET
    @Path("refresh/tenant-listener")
    public String getRefreshTokenTenantListener() {
        return getRefreshToken();
    }

    @GET
    @Path("refresh-query")
    public String getRefreshTokenQuery(@QueryParam("a") String aValue) {
        return getRefreshToken() + ":" + aValue;
    }
}
