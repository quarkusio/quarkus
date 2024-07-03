package io.quarkus.oidc.client;

import java.time.Duration;

import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;

/**
 * Access and Refresh tokens returned from a token grant request
 */
public class Tokens {
    private static final Logger LOG = Logger.getLogger(Tokens.class);

    final private String accessToken;
    final private Long accessTokenExpiresAt;
    final private Long refreshTokenTimeSkew;
    final private String refreshToken;
    final Long refreshTokenExpiresAt;
    final private JsonObject grantResponse;
    final private String clientId;

    public Tokens(String accessToken, Long accessTokenExpiresAt, Duration refreshTokenTimeSkewDuration, String refreshToken,
            Long refreshTokenExpiresAt, JsonObject grantResponse, String clientId) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenTimeSkew = refreshTokenTimeSkewDuration == null ? null : refreshTokenTimeSkewDuration.getSeconds();
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.grantResponse = grantResponse;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String get(String propertyName) {
        return grantResponse.getString(propertyName);
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public Long getRefreshTokenTimeSkew() {
        return refreshTokenTimeSkew;
    }

    public boolean isAccessTokenExpired() {
        return isExpired(accessTokenExpiresAt, true);
    }

    public boolean isRefreshTokenExpired() {
        return isExpired(refreshTokenExpiresAt, false);
    }

    public boolean isAccessTokenWithinRefreshInterval() {
        if (accessTokenExpiresAt == null || refreshTokenTimeSkew == null) {
            return false;
        }
        final long nowSecs = System.currentTimeMillis() / 1000;
        final boolean proactiveRefresh = nowSecs + refreshTokenTimeSkew > accessTokenExpiresAt;

        if (proactiveRefresh) {
            LOG.debugf(
                    "Access token is still valid but must be refreshed by client %s because it will expire in about %d seconds"
                            + " which is less than the refresh token time skew %d",
                    clientId, accessTokenExpiresAt - nowSecs, refreshTokenTimeSkew);
        }

        return proactiveRefresh;
    }

    private boolean isExpired(Long expiresAt, boolean accessToken) {
        if (expiresAt == null) {
            return false;
        }
        final long nowSecs = System.currentTimeMillis() / 1000;
        final boolean expired = nowSecs > expiresAt;

        if (expired) {
            if (accessToken) {
                LOG.debugf("Access token has expired and must be refreshed by client %s", clientId);
            } else {
                LOG.debugf("Refresh token for client %s has expired", clientId);
            }
        } else {
            final long expiresIn = expiresAt - nowSecs;
            if (accessToken) {
                LOG.tracef("Access token for client %s is valid but will expire in about %d seconds", clientId, expiresIn);
            } else {
                LOG.tracef("Refresh token for client %s is valid but will expire in about %d seconds", clientId, expiresIn);
            }
        }

        return expired;
    }
}
