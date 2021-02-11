package io.quarkus.oidc.client;

/**
 * Access and Refresh tokens returned from a token grant request
 */
public class Tokens {
    final private String accessToken;
    final private Long accessTokenExpiresAt;
    final private String refreshToken;

    public Tokens(String accessToken, Long accessTokenExpiresAt, String refreshToken) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public boolean isAccessTokenExpired() {
        if (accessTokenExpiresAt == null) {
            return false;
        } else {
            final long now = System.currentTimeMillis() / 1000;
            return now > accessTokenExpiresAt;
        }
    }

}
