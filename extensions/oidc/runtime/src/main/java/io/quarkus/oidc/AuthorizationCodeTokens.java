package io.quarkus.oidc;

/**
 * Authorization Code Flow Session State
 */
public class AuthorizationCodeTokens {

    private String idToken;
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;

    public AuthorizationCodeTokens() {
    }

    public AuthorizationCodeTokens(String idToken, String accessToken, String refreshToken) {
        this(idToken, accessToken, refreshToken, null);
    }

    public AuthorizationCodeTokens(String idToken, String accessToken, String refreshToken, Long accessTokenExpiresIn) {
        this.idToken = idToken;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }

    /**
     * Get the ID token
     *
     * @return ID token
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Set the ID token
     *
     * @param idToken ID token
     */
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    /**
     * Get the access token
     *
     * @return the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Set the access token
     *
     * @param accessToken the access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Get the refresh token
     *
     * @return refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Set the refresh token
     *
     * @param refreshToken refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Get the access token expires_in value in seconds.
     * It is relative to the time the access token is issued at.
     *
     * @return access token expires_in value in seconds.
     */
    public Long getAccessTokenExpiresIn() {
        return accessTokenExpiresIn;
    }

    /**
     * Set the access token expires_in value in seconds.
     * It is relative to the time the access token is issued at.
     * This property is only checked when an authorization code flow grant completes and does not have to be persisted..
     *
     * @param accessTokenExpiresIn access token expires_in value in seconds.
     */
    public void setAccessTokenExpiresIn(Long accessTokenExpiresIn) {
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }
}
