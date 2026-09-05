package io.quarkus.oidc.runtime;

import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * Default TokenIntrospection and UserInfo Cache implementation.
 * A single cache entry can keep TokenIntrospection and/or UserInfo
 * and is keyed by a composite key combining the tenant id and the access token.
 * <p>
 * In most cases it is the opaque bearer access tokens which are introspected
 * but the code flow access tokens can also be introspected if they have the roles claims.
 * <p>
 * In either case, if a remote request to fetch UserInfo is required then it will be the same access token
 * which has been introspected which will be used to request UserInfo.
 */
public class DefaultTokenIntrospectionUserInfoCache implements TokenIntrospectionCache, UserInfoCache {
    private static final Logger LOG = Logger.getLogger(DefaultTokenIntrospectionUserInfoCache.class);
    private static final Uni<TokenIntrospection> NULL_INTROSPECTION_UNI = Uni.createFrom().nullItem();
    private static final Uni<UserInfo> NULL_USERINFO_UNI = Uni.createFrom().nullItem();
    private static final String CACHE_KEY_DELIM = "|";

    final MemoryCache<CacheEntry> cache;

    public DefaultTokenIntrospectionUserInfoCache(OidcConfig oidcConfig, Vertx vertx) {
        cache = new MemoryCache<CacheEntry>(vertx, oidcConfig.tokenCache().cleanUpTimerInterval(),
                oidcConfig.tokenCache().timeToLive(), oidcConfig.tokenCache().maxSize());
    }

    @Override
    public Uni<Void> addIntrospection(String token, TokenIntrospection introspection, OidcTenantConfig oidcTenantConfig,
            OidcRequestContext<Void> requestContext) {
        final String key = getCacheKey(oidcTenantConfig, token);
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.introspection = introspection;
        } else {
            cache.add(key, new CacheEntry(introspection));
        }

        return CodeAuthenticationMechanism.VOID_UNI;
    }

    @Override
    public Uni<TokenIntrospection> getIntrospection(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<TokenIntrospection> requestContext) {
        final String key = getCacheKey(oidcConfig, token);
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.introspection == null) {
            return NULL_INTROSPECTION_UNI;
        }
        if (isTokenExpired(entry.introspection.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP), oidcConfig)) {
            LOG.debug("Introspected token has expired, removing it from the token introspection cache");
            cache.remove(key);
            return NULL_INTROSPECTION_UNI;
        }

        return Uni.createFrom().item(entry.introspection);
    }

    private static boolean isTokenExpired(Long exp, OidcTenantConfig oidcConfig) {
        final long lifespanGrace = oidcConfig != null ? oidcConfig.token().lifespanGrace().orElse(0) : 0;
        return exp != null
                && System.currentTimeMillis() / 1000 > (exp + lifespanGrace);
    }

    @Override
    public Uni<Void> addUserInfo(String token, UserInfo userInfo, OidcTenantConfig oidcTenantConfig,
            OidcRequestContext<Void> requestContext) {
        final String key = getCacheKey(oidcTenantConfig, token);
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.userInfo = userInfo;
        } else {
            cache.add(key, new CacheEntry(userInfo));
        }

        return CodeAuthenticationMechanism.VOID_UNI;
    }

    @Override
    public Uni<UserInfo> getUserInfo(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<UserInfo> requestContext) {
        final String key = getCacheKey(oidcConfig, token);
        CacheEntry entry = cache.get(key);
        return entry == null ? NULL_USERINFO_UNI : Uni.createFrom().item(entry.userInfo);
    }

    private static class CacheEntry {
        volatile TokenIntrospection introspection;
        volatile UserInfo userInfo;

        public CacheEntry(TokenIntrospection introspection) {
            this.introspection = introspection;
        }

        public CacheEntry(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }

    public void clearCache() {
        cache.clearCache();
    }

    public int getCacheSize() {
        return cache.getCacheSize();
    }

    public boolean containsCacheKey(String tenantId, String token) {
        return cache.containsKey(tenantId + CACHE_KEY_DELIM + token);
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        cache.stopTimer(vertx);
    }

    private static String getCacheKey(OidcTenantConfig oidcConfig, String token) {
        return oidcConfig.tenantId().get() + CACHE_KEY_DELIM + token;
    }
}
