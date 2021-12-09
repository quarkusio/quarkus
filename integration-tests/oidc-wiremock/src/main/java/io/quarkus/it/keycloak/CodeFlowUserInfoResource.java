package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.security.Authenticated;

@Path("/code-flow-user-info")
@Authenticated
public class CodeFlowUserInfoResource {

    @Inject
    UserInfo userInfo;

    @Inject
    DefaultTokenIntrospectionUserInfoCache tokenCache;

    @GET
    public String access() {
        int cacheSize = tokenCache.getCacheSize();
        tokenCache.clearCache();
        return userInfo.getString("preferred_username") + ", cache size: "
                + cacheSize;
    }
}
