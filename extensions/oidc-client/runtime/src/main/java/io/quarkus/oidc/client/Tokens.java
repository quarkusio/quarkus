package io.quarkus.oidc.client;

import java.time.Duration;

import io.vertx.core.json.JsonObject;

/**
 * Access and Refresh tokens returned from a token grant request
 */
public class Tokens {
    final private String accessToken;
    final private Long accessTokenExpiresAt;
    final private Long refreshTokenTimeSkew;
    final private String refreshToken;
    final Long refreshTokenExpiresAt;
    final private JsonObject grantResponse;

    public Tokens(String accessToken, Long accessTokenExpiresAt, Duration refreshTokenTimeSkewDuration, String refreshToken,
            Long refreshTokenExpiresAt, JsonObject grantResponse) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenTimeSkew = refreshTokenTimeSkewDuration == null ? null : refreshTokenTimeSkewDuration.getSeconds();
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.grantResponse = grantResponse;
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
        return isExpired(accessTokenExpiresAt);
    }

    public boolean isRefreshTokenExpired() {
        return isExpired(refreshTokenExpiresAt);
    }

    public boolean isAccessTokenWithinRefreshInterval() {
        if (accessTokenExpiresAt == null || refreshTokenTimeSkew == null) {
            return false;
        }
        final long nowSecs = System.currentTimeMillis() / 1000;
        return nowSecs + refreshTokenTimeSkew > accessTokenExpiresAt;
    }

    private static boolean isExpired(Long expiresAt) {
        if (expiresAt == null) {
            return false;
        }
        final long nowSecs = System.currentTimeMillis() / 1000;
        return nowSecs > expiresAt;
    }
}
