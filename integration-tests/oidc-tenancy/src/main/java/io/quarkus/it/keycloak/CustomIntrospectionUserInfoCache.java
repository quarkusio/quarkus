package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@AlternativePriority(1)
public class CustomIntrospectionUserInfoCache implements TokenIntrospectionCache, UserInfoCache {
    @Inject
    DefaultTokenIntrospectionUserInfoCache tokenCache;

    @Override
    public Uni<Void> addIntrospection(String token, TokenIntrospection introspection, OidcTenantConfig oidcConfig,
            OidcRequestContext<Void> requestContext) {
        return tokenCache.addIntrospection(token, introspection, oidcConfig, requestContext);
    }

    @Override
    public Uni<TokenIntrospection> getIntrospection(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<TokenIntrospection> requestContext) {
        return tokenCache.getIntrospection(token, oidcConfig, requestContext);
    }

    @Override
    public Uni<Void> addUserInfo(String token, UserInfo userInfo, OidcTenantConfig oidcConfig,
            OidcRequestContext<Void> requestContext) {
        return requestContext
                .runBlocking(() -> tokenCache.addUserInfo(token, userInfo, oidcConfig, requestContext).await().indefinitely());
    }

    @Override
    public Uni<UserInfo> getUserInfo(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<UserInfo> requestContext) {
        return tokenCache.getUserInfo(token, oidcConfig, requestContext);
    }

    public int getCacheSize() {
        return tokenCache.getCacheSize();
    }

    public void clearCache() {
        tokenCache.clearCache();
    }
}