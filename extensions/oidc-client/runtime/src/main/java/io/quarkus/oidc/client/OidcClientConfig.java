package io.quarkus.oidc.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcClientConfig extends OidcCommonConfig {

    /**
     * A unique OIDC client identifier. It must be set when OIDC clients are created dynamically
     * and is optional in all other cases.
     */
    @ConfigItem
    public Optional<String> id = Optional.empty();

    /**
     * If this client configuration is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean clientEnabled = true;

    /**
     * List of access token scopes
     */
    @ConfigItem
    public Optional<List<String>> scopes = Optional.empty();

    /**
     * Refresh token time skew in seconds.
     * If this property is enabled then the configured number of seconds is added to the current time
     * when checking whether the access token should be refreshed. If the sum is greater than this access token's
     * expiration time then a refresh is going to happen.
     */
    @ConfigItem
    public Optional<Duration> refreshTokenTimeSkew = Optional.empty();

    /**
     * If the access token 'expires_in' property should be checked as an absolute time value
     * as opposed to a duration relative to the current time.
     */
    @ConfigItem(defaultValue = "false")
    public boolean absoluteExpiresIn;

    public Grant grant = new Grant();

    @ConfigGroup
    public static class Grant {
        public static enum Type {
            /**
             * 'client_credentials' grant requiring an OIDC client authentication only
             */
            CLIENT("client_credentials"),
            /**
             * 'password' grant requiring both OIDC client and user ('username' and 'password') authentications
             */
            PASSWORD("password"),
            /**
             * 'authorization_code' grant requiring an OIDC client authentication as well as
             * at least 'code' and 'redirect_uri' parameters which must be passed to OidcClient at the token request time.
             */
            CODE("authorization_code"),
            /**
             * 'urn:ietf:params:oauth:grant-type:token-exchange' grant requiring an OIDC client authentication as well as
             * at least 'subject_token' parameter which must be passed to OidcClient at the token request time.
             */
            EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange"),
            /**
             * 'urn:ietf:params:oauth:grant-type:jwt-bearer' grant requiring an OIDC client authentication as well as
             * at least an 'assertion' parameter which must be passed to OidcClient at the token request time.
             */
            JWT("urn:ietf:params:oauth:grant-type:jwt-bearer"),
            /**
             * 'refresh_token' grant requiring an OIDC client authentication and a refresh token.
             * Note, OidcClient supports this grant by default if an access token acquisition response contained a refresh
             * token.
             * However, in some cases, the refresh token is provided out of band, for example, it can be shared between
             * several of the confidential client's services, etc.
             * If 'quarkus.oidc-client.grant-type' is set to 'refresh' then `OidcClient` will only support refreshing the
             * tokens.
             */
            REFRESH("refresh_token"),
            /**
             * 'urn:openid:params:grant-type:ciba' grant requiring an OIDC client authentication as well as 'auth_req_id'
             * parameter which must be passed to OidcClient at the token request time.
             */
            CIBA("urn:openid:params:grant-type:ciba");

            private String grantType;

            private Type(String grantType) {
                this.grantType = grantType;
            }

            public String getGrantType() {
                return grantType;
            }
        }

        /**
         * Grant type
         */
        @ConfigItem(defaultValue = "client")
        public Type type = Type.CLIENT;

        /**
         * Access token property name in a token grant response
         */
        @ConfigItem(defaultValue = OidcConstants.ACCESS_TOKEN_VALUE)
        public String accessTokenProperty = OidcConstants.ACCESS_TOKEN_VALUE;

        /**
         * Refresh token property name in a token grant response
         */
        @ConfigItem(defaultValue = OidcConstants.REFRESH_TOKEN_VALUE)
        public String refreshTokenProperty = OidcConstants.REFRESH_TOKEN_VALUE;

        /**
         * Access token expiry property name in a token grant response
         */
        @ConfigItem(defaultValue = OidcConstants.EXPIRES_IN)
        public String expiresInProperty = OidcConstants.EXPIRES_IN;

        /**
         * Refresh token expiry property name in a token grant response
         */
        @ConfigItem(defaultValue = OidcConstants.REFRESH_EXPIRES_IN)
        public String refreshExpiresInProperty = OidcConstants.REFRESH_EXPIRES_IN;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getAccessTokenProperty() {
            return accessTokenProperty;
        }

        public void setAccessTokenProperty(String accessTokenProperty) {
            this.accessTokenProperty = accessTokenProperty;
        }

        public String getRefreshTokenProperty() {
            return refreshTokenProperty;
        }

        public void setRefreshTokenProperty(String refreshTokenProperty) {
            this.refreshTokenProperty = refreshTokenProperty;
        }

        public String getExpiresInProperty() {
            return expiresInProperty;
        }

        public void setExpiresInProperty(String expiresInProperty) {
            this.expiresInProperty = expiresInProperty;
        }

        public String getRefreshExpiresInProperty() {
            return refreshExpiresInProperty;
        }

        public void setRefreshExpiresInProperty(String refreshExpiresInProperty) {
            this.refreshExpiresInProperty = refreshExpiresInProperty;
        }
    }

    /**
     * Grant options
     */
    @ConfigItem
    public Map<String, Map<String, String>> grantOptions;

    /**
     * Requires that all filters which use 'OidcClient' acquire the tokens at the post-construct initialization time,
     * possibly long before these tokens are used.
     * This property should be disabled if the access token may expire before it is used for the first time and no refresh token
     * is available.
     */
    @ConfigItem(defaultValue = "true")
    public boolean earlyTokensAcquisition = true;

    /**
     * Custom HTTP headers which have to be sent to the token endpoint
     */
    @ConfigItem
    public Map<String, String> headers;

    public Optional<String> getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Optional.of(id);
    }

    public Map<String, Map<String, String>> getGrantOptions() {
        return grantOptions;
    }

    public void setGrantOptions(Map<String, Map<String, String>> grantOptions) {
        this.grantOptions = grantOptions;
    }

    public boolean isClientEnabled() {
        return clientEnabled;
    }

    public void setClientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
    }

    public Optional<List<String>> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = Optional.of(scopes);
    }

    public Optional<Duration> getRefreshTokenTimeSkew() {
        return refreshTokenTimeSkew;
    }

    public void setRefreshTokenTimeSkew(Duration refreshTokenTimeSkew) {
        this.refreshTokenTimeSkew = Optional.of(refreshTokenTimeSkew);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isAbsoluteExpiresIn() {
        return absoluteExpiresIn;
    }

    public void setAbsoluteExpiresIn(boolean absoluteExpiresIn) {
        this.absoluteExpiresIn = absoluteExpiresIn;
    }

    public void setGrant(Grant grant) {
        this.grant = grant;
    }

    public Grant getGrant() {
        return grant;
    }
}
