package io.quarkus.it.keycloak;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Path("/")
@Authenticated
public class CodeFlowUserInfoResource {

    // UserInfo stubs are available for code flow JWT and binary access tokens and bearer JWT tokens
    @Inject
    UserInfo userInfo;

    // Custom test augmentor changes Principal to use UserInfo which has a preferred `alice` name
    @Inject
    SecurityIdentity identity;

    // current access token
    @Inject
    JsonWebToken accessToken;

    @Inject
    DefaultTokenIntrospectionUserInfoCache tokenCache;

    @Inject
    RefreshToken refreshToken;

    @GET
    @Path("/code-flow-user-info-only")
    public String access() {
        RoutingContext routingContext = identity.getAttribute(RoutingContext.class.getName());
        return identity.getPrincipal().getName() + ":" + userInfo.getPreferredUserName() + ":" + accessToken.getName()
                + ", cache size: "
                + tokenCache.getCacheSize()
                + ", TenantConfigResolver: " + Boolean.valueOf(routingContext.get("tenant-config-resolver"));
    }

    @GET
    @Path("/code-flow-user-info-github")
    public String accessGitHub() {
        return access();
    }

    @GET
    @Path("/code-flow-user-info-github-cached-in-idtoken")
    public String accessGitHubCachedInIdToken() {
        return access() +
                (refreshToken.getToken() != null ? ", refresh_token:" + refreshToken.getToken() : "");
    }

    @GET
    @Path("/code-flow-user-info-github-cache-disabled")
    public String accessGitHubCacheDisabled() {
        return access();
    }

    @GET
    @Path("/code-flow-user-info-dynamic-github")
    public String accessDynamicGitHub() {
        return access();
    }

    @GET
    @PermitAll
    @Path("/clear-token-cache")
    public void clearTokenCache() {
        tokenCache.clearCache();
    }
}
