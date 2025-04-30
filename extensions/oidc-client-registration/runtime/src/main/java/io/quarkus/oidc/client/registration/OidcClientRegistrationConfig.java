package io.quarkus.oidc.client.registration;

import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.smallrye.config.WithDefault;

//https://datatracker.ietf.org/doc/html/rfc7592
//https://openid.net/specs/openid-connect-registration-1_0.html

public interface OidcClientRegistrationConfig extends OidcCommonConfig {

    /**
     * OIDC Client Registration id
     */
    Optional<String> id();

    /**
     * If this client registration configuration is enabled.
     */
    @WithDefault("true")
    boolean registrationEnabled();

    /**
     * If the client configured with {@link #metadata} must be registered at startup.
     */
    @WithDefault("true")
    boolean registerEarly();

    /**
     * Initial access token
     */
    Optional<String> initialToken();

    /**
     * Client metadata
     */
    Metadata metadata();

    /**
     * Client metadata
     */
    interface Metadata {
        /**
         * Client name
         */
        Optional<String> clientName();

        /**
         * Redirect URI
         */
        Optional<String> redirectUri();

        /**
         * Post Logout URI
         */
        Optional<String> postLogoutUri();

        /**
         * Additional metadata properties
         */
        Map<String, String> extraProps();
    }

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with documented default values.
     *
     * @return OidcClientRegistrationConfigBuilder builder
     */
    static OidcClientRegistrationConfigBuilder builder() {
        return new OidcClientRegistrationConfigBuilder();
    }

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with {@code config} values.
     *
     * @param config client registration config; must not be null
     * @return OidcClientRegistrationConfigBuilder
     */
    static OidcClientRegistrationConfigBuilder builder(OidcClientRegistrationConfig config) {
        return new OidcClientRegistrationConfigBuilder(config);
    }

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with documented default values.
     *
     * @param authServerUrl {@link OidcCommonConfig#authServerUrl()}
     * @return OidcClientRegistrationConfigBuilder builder
     */
    static OidcClientRegistrationConfigBuilder authServerUrl(String authServerUrl) {
        return builder().authServerUrl(authServerUrl);
    }

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with documented default values.
     *
     * @param registrationPath {@link OidcCommonConfig#registrationPath()}
     * @return OidcClientRegistrationConfigBuilder builder
     */
    static OidcClientRegistrationConfigBuilder registrationPath(String registrationPath) {
        return builder().registrationPath(registrationPath);
    }
}
