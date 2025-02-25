package io.quarkus.oidc.runtime;

import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.Vertx;

public class BackChannelLogoutTokenCache {

    final MemoryCache<TokenVerificationResult> cache;

    public BackChannelLogoutTokenCache(OidcTenantConfig oidcTenantConfig, Vertx vertx) {
        cache = new MemoryCache<TokenVerificationResult>(vertx, oidcTenantConfig.logout().backchannel().cleanUpTimerInterval(),
                oidcTenantConfig.logout().backchannel().tokenCacheTimeToLive(),
                oidcTenantConfig.logout().backchannel().tokenCacheSize());
    }

    public void addTokenVerification(String token, TokenVerificationResult result) {
        cache.add(token, result);
    }

    public TokenVerificationResult removeTokenVerification(String token) {
        return cache.remove(token);
    }

    public boolean containsTokenVerification(String token) {
        return cache.containsKey(token);
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        cache.stopTimer(vertx);
    }
}
