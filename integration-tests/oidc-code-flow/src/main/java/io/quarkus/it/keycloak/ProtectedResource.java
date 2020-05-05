package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.Authenticated;

@Path("/web-app")
@Authenticated
public class ProtectedResource {

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

    @GET
    public String getName() {
        if (!idTokenCredential.getToken().equals(idToken.getRawToken())) {
            throw new OIDCException("ID token values are not equal");
        }
        return idToken.getName();
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
        if (!accessTokenCredential.getToken().equals(accessToken.getRawToken())) {
            throw new OIDCException("Access token values are not equal");
        }
        return accessToken.getRawToken() != null && !accessToken.getRawToken().isEmpty() ? "AT injected" : "";
    }

    @GET
    @Path("refresh")
    public String refresh() {
        if (!accessTokenCredential.getRefreshToken().getToken().equals(refreshToken.getToken())) {
            throw new OIDCException("Refresh token values are not equal");
        }
        return refreshToken.getToken() != null && !refreshToken.getToken().isEmpty() ? "RT injected" : "no refresh";
    }
}
