package io.quarkus.oidc.client.registration.runtime;

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
}
