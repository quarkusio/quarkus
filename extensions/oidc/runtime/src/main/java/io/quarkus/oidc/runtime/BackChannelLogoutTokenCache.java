package io.quarkus.oidc.runtime;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.oidc.OidcTenantConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class BackChannelLogoutTokenCache {
    private OidcTenantConfig oidcConfig;

    private Map<String, CacheEntry> cacheMap = new ConcurrentHashMap<>();;
    private AtomicInteger size = new AtomicInteger();

    public BackChannelLogoutTokenCache(OidcTenantConfig oidcTenantConfig, Vertx vertx) {
        this.oidcConfig = oidcTenantConfig;
        init(vertx);
    }

    private void init(Vertx vertx) {
        cacheMap = new ConcurrentHashMap<>();
        if (oidcConfig.logout.backchannel.cleanUpTimerInterval.isPresent()) {
            vertx.setPeriodic(oidcConfig.logout.backchannel.cleanUpTimerInterval.get().toMillis(), new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    // Remove all the entries which have expired
                    removeInvalidEntries();
                }
            });
        }
    }

    public void addTokenVerification(String token, TokenVerificationResult result) {
        if (!prepareSpaceForNewCacheEntry()) {
            clearCache();
        }
        cacheMap.put(token, new CacheEntry(result));
    }

    public TokenVerificationResult removeTokenVerification(String token) {
        CacheEntry entry = removeCacheEntry(token);
        return entry == null ? null : entry.result;
    }

    public boolean containsTokenVerification(String token) {
        return cacheMap.containsKey(token);
    }

    public void clearCache() {
        cacheMap.clear();
        size.set(0);
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
            if (currentSize == oidcConfig.logout.backchannel.tokenCacheSize) {
                return false;
            }
        } while (!size.compareAndSet(currentSize, currentSize + 1));
        return true;
    }

    private CacheEntry removeCacheEntry(String token) {
        CacheEntry entry = cacheMap.remove(token);
        if (entry != null) {
            size.decrementAndGet();
        }
        return entry;
    }

    private boolean isEntryExpired(CacheEntry entry, long now) {
        return entry.createdTime + oidcConfig.logout.backchannel.tokenCacheTimeToLive.toMillis() < now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static class CacheEntry {
        volatile TokenVerificationResult result;
        long createdTime = System.currentTimeMillis();

        public CacheEntry(TokenVerificationResult result) {
            this.result = result;
        }
    }
}
