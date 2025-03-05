package io.quarkus.it.oidc.dev.services;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;

@Path("secured")
public class SecuredResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    UserInfo userInfo;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.application-type", defaultValue = "service")
    String applicationType;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String serverUrl;

    @RolesAllowed("admin")
    @GET
    @Path("admin-only")
    public String getAdminOnly() {
        return (isWebApp() ? idToken.getName() : securityIdentity.getPrincipal().getName()) + " " + securityIdentity.getRoles();
    }

    @RolesAllowed("user")
    @GET
    @Path("user-only")
    public String getUserOnly() {
        return userInfo.getPreferredUserName() + " " + securityIdentity.getRoles();
    }

    @GET
    @Path("auth-server-url")
    public String getAuthServerUrl() {
        return serverUrl;
    }

    private boolean isWebApp() {
        return "web-app".equals(applicationType);
    }
}
