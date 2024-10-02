package io.quarkus.oidc.client.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

//https://datatracker.ietf.org/doc/html/rfc7592
//https://openid.net/specs/openid-connect-registration-1_0.html

@ConfigGroup
public class OidcClientRegistrationConfig extends OidcCommonConfig {

    /**
     * OIDC Client Registration id
     */
    @ConfigItem
    public Optional<String> id = Optional.empty();

    /**
     * If this client registration configuration is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean registrationEnabled = true;

    /**
     * If the client configured with {@link #metadata} must be registered at startup.
     */
    @ConfigItem(defaultValue = "true")
    public boolean registerEarly = true;

    /**
     * Initial access token
     */
    @ConfigItem
    public Optional<String> initialToken = Optional.empty();

    /**
     * Client metadata
     */
    @ConfigItem
    public Metadata metadata = new Metadata();

    /**
     * Client metadata
     */
    @ConfigGroup
    public static class Metadata {
        /**
         * Client name
         */
        @ConfigItem
        public Optional<String> clientName = Optional.empty();

        /**
         * Redirect URI
         */
        @ConfigItem
        public Optional<String> redirectUri = Optional.empty();

        /**
         * Post Logout URI
         */
        @ConfigItem
        public Optional<String> postLogoutUri = Optional.empty();

        /**
         * Additional metadata properties
         */
        @ConfigItem
        public Map<String, String> extraProps = new HashMap<>();
    }
}
