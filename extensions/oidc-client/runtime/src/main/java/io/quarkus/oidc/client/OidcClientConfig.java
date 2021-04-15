package io.quarkus.oidc.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;
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
            EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange");

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

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
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
}
