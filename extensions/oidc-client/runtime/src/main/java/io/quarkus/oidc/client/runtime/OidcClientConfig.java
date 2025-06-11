package io.quarkus.oidc.client.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfigBuilder;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface OidcClientConfig extends OidcClientCommonConfig {

    /**
     * A unique OIDC client identifier. It must be set when OIDC clients are created dynamically
     * and is optional in all other cases.
     */
    Optional<String> id();

    /**
     * If this client configuration is enabled.
     */
    @WithDefault("true")
    boolean clientEnabled();

    /**
     * List of access token scopes
     */
    Optional<List<String>> scopes();

    /**
     * Refresh token time skew.
     * If this property is enabled then the configured duration is converted to seconds and is added to the current time
     * when checking whether the access token should be refreshed. If the sum is greater than this access token's
     * expiration time then a refresh is going to happen.
     */
    Optional<Duration> refreshTokenTimeSkew();

    /**
     * Access token expiration period relative to the current time.
     * This property is only checked when an access token grant response
     * does not include an access token expiration property.
     */
    Optional<Duration> accessTokenExpiresIn();

    /**
     * Access token expiry time skew that can be added to the calculated token expiry time.
     */
    Optional<Duration> accessTokenExpirySkew();

    /**
     * If the access token 'expires_in' property should be checked as an absolute time value
     * as opposed to a duration relative to the current time.
     */
    @WithDefault("false")
    boolean absoluteExpiresIn();

    /**
     * OIDC Client grant config group.
     */
    Grant grant();

    interface Grant {
        enum Type {
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

            private final String grantType;

            Type(String grantType) {
                this.grantType = grantType;
            }

            public String getGrantType() {
                return grantType;
            }
        }

        /**
         * Grant type
         */
        @WithDefault("client")
        Type type();

        /**
         * Access token property name in a token grant response
         */
        @WithDefault(OidcConstants.ACCESS_TOKEN_VALUE)
        String accessTokenProperty();

        /**
         * Refresh token property name in a token grant response
         */
        @WithDefault(OidcConstants.REFRESH_TOKEN_VALUE)
        String refreshTokenProperty();

        /**
         * Access token expiry property name in a token grant response
         */
        @WithDefault(OidcConstants.EXPIRES_IN)
        String expiresInProperty();

        /**
         * Refresh token expiry property name in a token grant response
         */
        @WithDefault(OidcConstants.REFRESH_EXPIRES_IN)
        String refreshExpiresInProperty();
    }

    /**
     * Grant options
     */
    @ConfigDocMapKey("grant-name")
    Map<String, Map<String, String>> grantOptions();

    /**
     * Requires that all filters which use 'OidcClient' acquire the tokens at the post-construct initialization time,
     * possibly long before these tokens are used.
     * This property should be disabled if the access token may expire before it is used for the first time and no refresh token
     * is available.
     */
    @WithDefault("true")
    boolean earlyTokensAcquisition();

    /**
     * Custom HTTP headers which have to be sent to the token endpoint
     */
    Map<String, String> headers();

    /**
     * Token refresh interval.
     * By default, OIDC client refreshes the token during the current request, when it detects that it has expired,
     * or nearly expired if the {@link #refreshTokenTimeSkew()} is configured.
     * But, when this property is configured, OIDC client can refresh the token asynchronously in the configured interval.
     * This property is only effective with OIDC client filters and other {@link AbstractTokensProducer} extensions,
     * but not when you use the {@link OidcClient#getTokens()} API directly.
     */
    Optional<Duration> refreshInterval();

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with documented default values.
     *
     * @return OidcClientConfigBuilder builder
     */
    static OidcClientConfigBuilder builder() {
        return new OidcClientConfigBuilder();
    }

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with {@code config} values.
     *
     * @param config client config; must not be null
     * @return OidcClientConfigBuilder
     */
    static OidcClientConfigBuilder builder(OidcClientConfig config) {
        return new OidcClientConfigBuilder(config);
    }

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with documented default values.
     *
     * @param authServerUrl {@link OidcCommonConfig#authServerUrl()}
     * @return OidcClientConfigBuilder builder
     */
    static OidcClientConfigBuilder authServerUrl(String authServerUrl) {
        return builder().authServerUrl(authServerUrl);
    }

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with documented default values.
     *
     * @param registrationPath {@link OidcCommonConfig#registrationPath()}
     * @return OidcClientConfigBuilder builder
     */
    static OidcClientConfigBuilder registrationPath(String registrationPath) {
        return builder().registrationPath(registrationPath);
    }

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with documented default values.
     *
     * @param tokenPath {@link OidcClientCommonConfig#tokenPath()}
     * @return OidcClientConfigBuilder builder
     */
    static OidcClientConfigBuilder tokenPath(String tokenPath) {
        return builder().tokenPath(tokenPath);
    }

}
