package io.quarkus.oidc.runtime;

import java.time.Duration;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

public class JwkSetRefreshHandler implements Handler<String> {
    private static final Logger LOG = Logger.getLogger(JwkSetRefreshHandler.class);
    private OAuth2Auth auth;
    private volatile long lastForcedRefreshTime;
    private volatile long forcedJwksRefreshIntervalMilliSecs;

    public JwkSetRefreshHandler(OAuth2Auth auth, Duration forcedJwksRefreshInterval) {
        this.auth = auth;
        this.forcedJwksRefreshIntervalMilliSecs = forcedJwksRefreshInterval.toMillis();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handle(String kid) {
        final long now = System.currentTimeMillis();
        if (now > lastForcedRefreshTime + forcedJwksRefreshIntervalMilliSecs) {
            lastForcedRefreshTime = now;
            LOG.debugf("No JWK with %s key id is available, trying to refresh the JWK set", kid);
            auth.loadJWK(res -> {
                if (res.failed()) {
                    LOG.debugf("Failed to refresh the JWK set: %s", res.cause());
                }
            });
        }
    }
}
