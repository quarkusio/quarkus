package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OIDCException;

@Path("/tenant/{tenant}/api/user")
public class TenantResource {

    @Inject
    JsonWebToken accessToken;

    @Inject
    AccessTokenCredential accessTokenCred;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @GET
    @RolesAllowed("user")
    public String userName(@PathParam("tenant") String tenant) {
        return tenant + ":" + (tenant.startsWith("tenant-web-app") ? getNameWebAppType() : getNameServiceType());
    }

    private String getNameWebAppType() {
        if (!"ID".equals(idToken.getClaim("typ"))) {
            throw new OIDCException("Wrong ID token type");
        }
        String name = idToken.getName();
        // The test is set up to use 'upn' for the 'web-app' application type
        if (!name.equals(idToken.getClaim("upn"))) {
            throw new OIDCException("upn claim is missing");
        }
        // Access token must be available too
        if (!"Bearer".equals(accessToken.getClaim("typ"))) {
            throw new OIDCException("Wrong access token type");
        }
        if (accessTokenCred.isOpaque()) {
            throw new OIDCException("JWT token is expected");
        }
        return name;
    }

    private String getNameServiceType() {
        if (accessTokenCred.isOpaque()) {
            throw new OIDCException("JWT token is expected");
        }
        if (!"Bearer".equals(accessToken.getClaim("typ"))) {
            throw new OIDCException("Wrong access token type");
        }
        String name = null;
        try {
            name = idToken.getName();
        } catch (OIDCException ex) {
            // expected because an ID token must not be available
        }
        if (name != null) {
            throw new OIDCException("Only the access token can be availabe with the 'service' application type");
        }
        name = accessToken.getName();
        // The test is set up to use 'upn' for the 'web-app' application type
        if (!name.equals(accessToken.getClaim("preferred_username"))) {
            throw new OIDCException("preferred_username claim is missing");
        }
        return name;
    }

}
