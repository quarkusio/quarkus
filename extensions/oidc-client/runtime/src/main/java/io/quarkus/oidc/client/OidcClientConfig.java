package io.quarkus.oidc.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;

/**
 * @deprecated create {@link io.quarkus.oidc.client.runtime.OidcClientConfig} with the {@link OidcClientConfigBuilder}
 *             for example, you can use the {@link io.quarkus.oidc.client.runtime.OidcClientConfig#builder()} method.
 */
@Deprecated(since = "3.18", forRemoval = true)
public class OidcClientConfig extends OidcClientCommonConfig implements io.quarkus.oidc.client.runtime.OidcClientConfig {

    public OidcClientConfig() {
        this.refreshInterval = Optional.empty();
    }

    public OidcClientConfig(io.quarkus.oidc.client.runtime.OidcClientConfig mapping) {
        super(mapping);
        id = mapping.id();
        clientEnabled = mapping.clientEnabled();
        scopes = mapping.scopes();
        refreshTokenTimeSkew = mapping.refreshTokenTimeSkew();
        accessTokenExpiresIn = mapping.accessTokenExpiresIn();
        accessTokenExpirySkew = mapping.accessTokenExpirySkew();
        absoluteExpiresIn = mapping.absoluteExpiresIn();
        grant.addConfigMappingValues(mapping.grant());
        grantOptions = mapping.grantOptions();
        earlyTokensAcquisition = mapping.earlyTokensAcquisition();
        headers = mapping.headers();
        refreshInterval = mapping.refreshInterval();
    }

    /**
     * A unique OIDC client identifier. It must be set when OIDC clients are created dynamically
     * and is optional in all other cases.
     */
    public Optional<String> id = Optional.empty();

    /**
     * If this client configuration is enabled.
     */
    public boolean clientEnabled = true;

    /**
     * List of access token scopes
     */
    public Optional<List<String>> scopes = Optional.empty();

    /**
     * Refresh token time skew.
     * If this property is enabled then the configured duration is converted to seconds and is added to the current time
     * when checking whether the access token should be refreshed. If the sum is greater than this access token's
     * expiration time then a refresh is going to happen.
     */
    public Optional<Duration> refreshTokenTimeSkew = Optional.empty();

    /**
     * Access token expiration period relative to the current time.
     * This property is only checked when an access token grant response
     * does not include an access token expiration property.
     */
    public Optional<Duration> accessTokenExpiresIn = Optional.empty();

    /**
     * Access token expiry time skew that can be added to the calculated token expiry time.
     */
    public Optional<Duration> accessTokenExpirySkew = Optional.empty();

    /**
     * If the access token 'expires_in' property should be checked as an absolute time value
     * as opposed to a duration relative to the current time.
     */
    public boolean absoluteExpiresIn;

    public Grant grant = new Grant();

    private final Optional<Duration> refreshInterval;

    @Override
    public Optional<String> id() {
        return id;
    }

    @Override
    public boolean clientEnabled() {
        return clientEnabled;
    }

    @Override
    public Optional<List<String>> scopes() {
        return scopes;
    }

    @Override
    public Optional<Duration> refreshTokenTimeSkew() {
        return refreshTokenTimeSkew;
    }

    @Override
    public Optional<Duration> accessTokenExpiresIn() {
        return accessTokenExpiresIn;
    }

    @Override
    public Optional<Duration> accessTokenExpirySkew() {
        return accessTokenExpirySkew;
    }

    @Override
    public boolean absoluteExpiresIn() {
        return absoluteExpiresIn;
    }

    @Override
    public io.quarkus.oidc.client.runtime.OidcClientConfig.Grant grant() {
        return grant;
    }

    @Override
    public Map<String, Map<String, String>> grantOptions() {
        return grantOptions;
    }

    @Override
    public boolean earlyTokensAcquisition() {
        return earlyTokensAcquisition;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public Optional<Duration> refreshInterval() {
        return refreshInterval;
    }

    public static class Grant implements io.quarkus.oidc.client.runtime.OidcClientConfig.Grant {

        @Override
        public io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type type() {
            return type == null ? null : io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type.valueOf(type.toString());
        }

        @Override
        public String accessTokenProperty() {
            return accessTokenProperty;
        }

        @Override
        public String refreshTokenProperty() {
            return refreshTokenProperty;
        }

        @Override
        public String expiresInProperty() {
            return expiresInProperty;
        }

        @Override
        public String refreshExpiresInProperty() {
            return refreshExpiresInProperty;
        }

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
            CIBA("urn:openid:params:grant-type:ciba"),
            /**
             * 'urn:ietf:params:oauth:grant-type:device_code' grant requiring an OIDC client authentication as well as
             * 'device_code'
             * parameter which must be passed to OidcClient at the token request time.
             */
            DEVICE("urn:ietf:params:oauth:grant-type:device_code");

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
        public Type type = Type.CLIENT;

        /**
         * Access token property name in a token grant response
         */
        public String accessTokenProperty = OidcConstants.ACCESS_TOKEN_VALUE;

        /**
         * Refresh token property name in a token grant response
         */
        public String refreshTokenProperty = OidcConstants.REFRESH_TOKEN_VALUE;

        /**
         * Access token expiry property name in a token grant response
         */
        public String expiresInProperty = OidcConstants.EXPIRES_IN;

        /**
         * Refresh token expiry property name in a token grant response
         */
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

        private void addConfigMappingValues(io.quarkus.oidc.client.runtime.OidcClientConfig.Grant grant) {
            this.type = Grant.Type.valueOf(grant.type().toString());
            this.accessTokenProperty = grant.accessTokenProperty();
            this.refreshTokenProperty = grant.refreshTokenProperty();
            this.expiresInProperty = grant.expiresInProperty();
            this.refreshExpiresInProperty = grant.refreshExpiresInProperty();
        }
    }

    /**
     * Grant options
     */
    public Map<String, Map<String, String>> grantOptions;

    /**
     * Requires that all filters which use 'OidcClient' acquire the tokens at the post-construct initialization time,
     * possibly long before these tokens are used.
     * This property should be disabled if the access token may expire before it is used for the first time and no refresh token
     * is available.
     */
    public boolean earlyTokensAcquisition = true;

    /**
     * Custom HTTP headers which have to be sent to the token endpoint
     */
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
