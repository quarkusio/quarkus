package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/")
@Authenticated
public class CodeFlowUserInfoResource {

    @Inject
    UserInfo userInfo;

    @Inject
    SecurityIdentity identity;

    @Inject
    DefaultTokenIntrospectionUserInfoCache tokenCache;

    @GET
    @Path("/code-flow-user-info-only")
    public String access() {
        int cacheSize = tokenCache.getCacheSize();
        tokenCache.clearCache();
        return identity.getPrincipal().getName() + ":" + userInfo.getString("preferred_username") + ", cache size: "
                + cacheSize;
    }

    @GET
    @Path("/code-flow-user-info-github")
    public String accessGitHub() {
        return access();
    }

    @GET
    @Path("/code-flow-user-info-github-cached-in-idtoken")
    public String accessGitHubCachedInIdToken() {
        return access();
    }

    @GET
    @Path("/code-flow-user-info-dynamic-github")
    public String accessDynamicGitHub() {
        return access();
    }
}
