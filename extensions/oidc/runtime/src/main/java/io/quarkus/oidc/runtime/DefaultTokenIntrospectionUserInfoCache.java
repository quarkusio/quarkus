package io.quarkus.oidc.runtime;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.runtime.OidcConfig.TokenCache;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Default TokenIntrospection and UserInfo Cache implementation.
 * A single cache entry can keep TokenIntrospection and/or UserInfo.
 * <p>
 * In most cases it is the opaque bearer access tokens which are introspected
 * but the code flow access tokens can also be introspected if they have the roles claims.
 * <p>
 * In either case, if a remote request to fetch UserInfo is required then it will be the same access token
 * which has been introspected which will be used to request UserInfo.
 */
public class DefaultTokenIntrospectionUserInfoCache implements TokenIntrospectionCache, UserInfoCache {
    private static final Uni<TokenIntrospection> NULL_INTROSPECTION_UNI = Uni.createFrom().nullItem();
    private static final Uni<UserInfo> NULL_USERINFO_UNI = Uni.createFrom().nullItem();

    private TokenCache cacheConfig;

    private Map<String, CacheEntry> cacheMap;
    private AtomicInteger size = new AtomicInteger();

    public DefaultTokenIntrospectionUserInfoCache(OidcConfig oidcConfig, Vertx vertx) {
        this.cacheConfig = oidcConfig.tokenCache;
        init(vertx);
    }

    private void init(Vertx vertx) {
        if (cacheConfig.maxSize > 0) {
            cacheMap = new ConcurrentHashMap<>();
            if (cacheConfig.cleanUpTimerInterval.isPresent()) {
                vertx.setPeriodic(cacheConfig.cleanUpTimerInterval.get().toMillis(), new Handler<Long>() {
                    @Override
                    public void handle(Long event) {
                        // Remove all the entries which have expired
                        removeInvalidEntries();
                    }
                });
            }
        } else {
            cacheMap = Collections.emptyMap();
        }
    }

    @Override
    public Uni<Void> addIntrospection(String token, TokenIntrospection introspection, OidcTenantConfig oidcTenantConfig,
            OidcRequestContext<Void> requestContext) {
        if (cacheConfig.maxSize > 0) {
            CacheEntry entry = findValidCacheEntry(token);
            if (entry != null) {
                entry.introspection = introspection;
            } else if (prepareSpaceForNewCacheEntry()) {
                cacheMap.put(token, new CacheEntry(introspection));
            }
        }

        return CodeAuthenticationMechanism.VOID_UNI;
    }

    @Override
    public Uni<TokenIntrospection> getIntrospection(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<TokenIntrospection> requestContext) {
        CacheEntry entry = findValidCacheEntry(token);
        return entry == null ? NULL_INTROSPECTION_UNI : Uni.createFrom().item(entry.introspection);
    }

    @Override
    public Uni<Void> addUserInfo(String token, UserInfo userInfo, OidcTenantConfig oidcTenantConfig,
            OidcRequestContext<Void> requestContext) {
        if (cacheConfig.maxSize > 0) {
            CacheEntry entry = findValidCacheEntry(token);
            if (entry != null) {
                entry.userInfo = userInfo;
            } else if (prepareSpaceForNewCacheEntry()) {
                cacheMap.put(token, new CacheEntry(userInfo));
            }
        }

        return CodeAuthenticationMechanism.VOID_UNI;
    }

    @Override
    public Uni<UserInfo> getUserInfo(String token, OidcTenantConfig oidcConfig,
            OidcRequestContext<UserInfo> requestContext) {
        CacheEntry entry = findValidCacheEntry(token);
        return entry == null ? NULL_USERINFO_UNI : Uni.createFrom().item(entry.userInfo);
    }

    public int getCacheSize() {
        return cacheMap.size();
    }

    public void clearCache() {
        cacheMap.clear();
    }

    private void removeInvalidEntries() {
        long now = now();
        for (Iterator<Map.Entry<String, CacheEntry>> it = cacheMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, CacheEntry> next = it.next();
            if (isEntryExpired(next.getValue(), now)) {
                it.remove();
                size.decrementAndGet();
            }
        }
    }

    private boolean prepareSpaceForNewCacheEntry() {
        int currentSize;
        do {
            currentSize = size.get();
            if (currentSize == cacheConfig.maxSize) {
                return false;
            }
        } while (!size.compareAndSet(currentSize, currentSize + 1));
        return true;
    }

    private CacheEntry findValidCacheEntry(String token) {
        CacheEntry entry = cacheMap.get(token);
        if (entry != null) {
            long now = now();
            if (isEntryExpired(entry, now)) {
                // Entry has expired, remote introspection will be required
                entry = null;
                cacheMap.remove(token);
                size.decrementAndGet();
            }
        }
        return entry;
    }

    private boolean isEntryExpired(CacheEntry entry, long now) {
        return entry.createdTime + cacheConfig.timeToLive.toMillis() < now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static class CacheEntry {
        volatile TokenIntrospection introspection;
        volatile UserInfo userInfo;
        long createdTime = System.currentTimeMillis();

        public CacheEntry(TokenIntrospection introspection) {
            this.introspection = introspection;
        }

        public CacheEntry(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }

    private static class IncrementOperator implements IntUnaryOperator {
        int maxSize;

        IncrementOperator(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public int applyAsInt(int n) {
            return n < maxSize ? n + 1 : n;
        }

    }
}
