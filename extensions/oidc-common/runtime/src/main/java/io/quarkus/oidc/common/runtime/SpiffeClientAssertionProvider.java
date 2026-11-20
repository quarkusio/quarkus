package io.quarkus.oidc.common.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.svid.jwt.WorkloadJsonWebToken;
import io.smallrye.config.Config;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

final class SpiffeClientAssertionProvider implements ClientAssertionProvider, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SpiffeClientAssertionProvider.class);
    private static final double REFRESH_RATIO = 0.85;
    private static final long EXPIRY_MARGIN_MS = 2000;
    private static final String SPIFFE_CLIENT_AUDIENCES_PROPERTY = "quarkus.spiffe-client.audiences";

    private record CachedToken(String token, long expiresAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMs;
        }
    }

    private final SpiffeClient spiffeClient;
    // null if the default audiences configured for the SPIFFE client are used
    private final String audience;
    private final Vertx vertx;
    private volatile CachedToken cachedToken;
    private volatile long timerId = -1;
    private volatile boolean closed;

    SpiffeClientAssertionProvider(SpiffeClient spiffeClient, String audience, Vertx vertx) {
        this.spiffeClient = spiffeClient;
        this.audience = audience;
        this.vertx = vertx;
    }

    /**
     * @param audience JWT-SVID audience, null if the default audiences configured for the SPIFFE client must be used
     */
    static SpiffeClientAssertionProvider forAudience(Vertx vertx, String audience) {
        SpiffeClient spiffeClient = Arc.requireContainer().select(SpiffeClient.class).orNull();
        if (spiffeClient == null) {
            return null;
        }
        return new SpiffeClientAssertionProvider(spiffeClient, audience, vertx);
    }

    /**
     * @return true if default audiences are configured for the SPIFFE client
     */
    // TODO: consider updating JwtSvidClient to return the configured default audiences instead of reading the property
    static boolean hasDefaultAudiences() {
        return Config.get().getOptionalValues(SPIFFE_CLIENT_AUDIENCES_PROPERTY, String.class)
                .filter(audiences -> !audiences.isEmpty()).isPresent();
    }

    @Override
    public Uni<String> getClientAssertion() {
        CachedToken cached = this.cachedToken;
        if (cached != null && !cached.isExpired()) {
            return Uni.createFrom().item(cached.token);
        }
        return fetchAndCache();
    }

    @Override
    public String getClientAssertionType() {
        return OidcConstants.SPIFFE_SVID_CLIENT_ASSERTION_TYPE;
    }

    @Override
    public void close() {
        closed = true;
        cancelRefresh();
        cachedToken = null;
    }

    private Uni<String> fetchAndCache() {
        return getWorkloadJsonWebToken()
                .map(svid -> {
                    cacheAndScheduleRefresh(svid);
                    return svid.token();
                });
    }

    private void cacheAndScheduleRefresh(WorkloadJsonWebToken svid) {
        if (closed) {
            return;
        }
        final long expiresAtMs = svid.expiry().toEpochMilli();
        cancelRefresh();
        cachedToken = new CachedToken(svid.token(), expiresAtMs - EXPIRY_MARGIN_MS);
        scheduleRefresh(expiresAtMs);
    }

    private void scheduleRefresh(long expiresAtMs) {
        final long ttlMs = expiresAtMs - System.currentTimeMillis();
        if (ttlMs <= 0) {
            return;
        }
        final long delayMs = (long) (ttlMs * REFRESH_RATIO);
        if (delayMs <= 0) {
            return;
        }
        timerId = vertx.setTimer(delayMs, ignored -> refreshToken());
    }

    private void refreshToken() {
        if (closed) {
            return;
        }
        getWorkloadJsonWebToken()
                .subscribe().with(
                        svid -> {
                            if (!closed) {
                                cacheAndScheduleRefresh(svid);
                            }
                        },
                        error -> LOG.warnf("Failed to refresh SPIFFE JWT-SVID for %s: %s",
                                audience != null ? "audience '" + audience + "'" : "the SPIFFE client default audiences",
                                error.getMessage()));
    }

    private Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken() {
        return audience != null ? spiffeClient.getWorkloadJsonWebToken(audience) : spiffeClient.getWorkloadJsonWebToken();
    }

    private void cancelRefresh() {
        final long id = timerId;
        if (id != -1) {
            vertx.cancelTimer(id);
            timerId = -1;
        }
    }
}
