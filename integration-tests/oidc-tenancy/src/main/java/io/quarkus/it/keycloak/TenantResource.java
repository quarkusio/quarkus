package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant/{tenant}/api/user")
public class TenantResource {

    @Inject
    JsonWebToken accessToken;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AccessTokenCredential accessTokenCred;

    @Inject
    CustomIntrospectionUserInfoCache tokenCache;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    OidcSession oidcSession;

    @Inject
    OidcClients oidcClients;

    @Inject
    RoutingContext routingContext;

    @GET
    @RolesAllowed("user")
    public String userNameService(@PathParam("tenant") String tenant, @QueryParam("revoke") boolean revokeToken) {
        if (tenant.startsWith("tenant-web-app")) {
            throw new OIDCException("Wrong tenant");
        }
        String name = getNameServiceType();
        if ("tenant-d".equals(tenant) || "tenant-b-no-discovery".equals(tenant)) {
            UserInfo userInfo = getUserInfo();
            if (!userInfo.contains(Claims.sub.name())) {
                throw new OIDCException("UserInfo returned from Keycloak must contain 'sub'");
            }
            if (userInfo.getPropertyNames().contains(Claims.preferred_username.name())) {
                name = name + "." + userInfo.getString(Claims.preferred_username.name());
            }
        }

        String response = tenant + ":" + name;
        if (tenant.startsWith("tenant-oidc-introspection-only")) {
            TokenIntrospection introspection = securityIdentity.getAttribute("introspection");
            response += (",client_id:" + introspection.getString("client_id"));
            response += (",introspection_client_id:" + introspection.getString("introspection_client_id"));
            response += (",introspection_client_secret:" + introspection.getString("introspection_client_secret"));
            response += (",active:" + introspection.getBoolean("active"));
            response += (",cache-size:" + tokenCache.getCacheSize());
        }

        if (revokeToken) {
            OidcClientConfig oidcClientConfig = new OidcClientConfig();
            oidcClientConfig.setClientId("client");
            oidcClientConfig.setId("clientId");
            oidcClientConfig.setTokenPath("http://localhost:8081/oidc/token");
            oidcClientConfig.setRevokePath("http://localhost:8081/oidc/revoke");

            oidcClients.newClient(oidcClientConfig)
                    .chain(oidcClient -> oidcClient.revokeAccessToken(accessTokenCred.getToken())).await().indefinitely();

        }

        return response;
    }

    @GET
    @RolesAllowed("user")
    @Path("no-discovery")
    public String userNameServiceNoDiscovery(@PathParam("tenant") String tenant) {
        return userNameService(tenant, false);
    }

    @GET
    @Path("webapp")
    @RolesAllowed("user")
    public String userNameWebApp(@PathParam("tenant") String tenant, @QueryParam("logout") boolean localLogout) {
        if (!tenant.equals("tenant-web-app") && !tenant.equals("tenant-web-app-dynamic")
                && !tenant.equals("tenant-web-app-no-discovery")) {
            throw new OIDCException("Wrong tenant");
        }
        if (!tenant.equals(oidcSession.getTenantId())) {
            throw new OIDCException("'tenant' parameter does not match the OIDC session tenantid");
        }
        UserInfo userInfo = getUserInfo();
        if (!idToken.getGroups().contains("user")) {
            throw new OIDCException("Groups expected");
        }

        if (!idToken.getRawToken().equals(oidcSession.getIdToken().getRawToken())) {
            throw new OIDCException("Wrong ID token injection");
        }

        String response = tenant + ":" + getNameWebAppType(userInfo.getString("upn"), "upn", "preferred_username");

        if (routingContext.get("reauthenticated") != null) {
            response += ":reauthenticated";
        }

        if (localLogout) {
            oidcSession.logout().await().indefinitely();
            response += ":logout";
        }
        return response;
    }

    @GET
    @Path("webapp-no-discovery")
    @RolesAllowed("user")
    public String userNameWebAppNoDiscovery(@PathParam("tenant") String tenant) {
        return userNameWebApp(tenant, false);
    }

    private UserInfo getUserInfo() {
        if (!(securityIdentity.getAttribute("userinfo") instanceof UserInfo)) {
            throw new OIDCException("userinfo attribute must be set");
        }
        // Not injecting in the service field as not all tenants require it
        return Arc.container().instance(UserInfo.class).get();
    }

    @GET
    @Path("webapp2")
    @RolesAllowed("user")
    public String userNameWebApp2(@PathParam("tenant") String tenant) {
        if (!tenant.equals("tenant-web-app2")) {
            throw new OIDCException("Wrong tenant");
        }
        if (idToken.getGroups().contains("user")) {
            throw new OIDCException("Groups are not expected");
        }
        return tenant + ":" + getNameWebAppType(idToken.getName(), "preferred_username", "upn");
    }

    private String getNameWebAppType(String name,
            String idTokenNameClaim,
            String idTokenNameClaimNotExpected) {
        if (!"ID".equals(idToken.getClaim("typ"))) {
            throw new OIDCException("Wrong ID token type");
        }
        if (!name.equals(idToken.getClaim(idTokenNameClaim))) {
            throw new OIDCException(idTokenNameClaim + " claim is missing");
        }
        if (idToken.getClaim(idTokenNameClaimNotExpected) != null) {
            throw new OIDCException(idTokenNameClaimNotExpected + " claim is not expected");
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
            throw new OIDCException("Only the access token can be available with the 'service' application type");
        }
        name = accessToken.getName();
        // The test is set up to use 'upn' for the 'web-app' application type
        if (!name.equals(accessToken.getClaim("preferred_username"))) {
            throw new OIDCException("preferred_username claim is missing");
        }
        return name;
    }

}
