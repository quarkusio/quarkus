package io.quarkus.oidc;

/**
 * Authorization Code Flow Session State
 */
public class AuthorizationCodeTokens {

    private String idToken;
    private String accessToken;
    private String refreshToken;

    public AuthorizationCodeTokens() {
    }

    public AuthorizationCodeTokens(String idToken, String accessToken, String refreshToken) {
        this.setIdToken(idToken);
        this.setAccessToken(accessToken);
        this.setRefreshToken(refreshToken);
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
