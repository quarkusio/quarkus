package io.quarkus.oidc.client.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;

//https://datatracker.ietf.org/doc/html/rfc7592
//https://openid.net/specs/openid-connect-registration-1_0.html

public class OidcClientRegistrationConfig extends OidcCommonConfig {

    public OidcClientRegistrationConfig() {

    }

    public OidcClientRegistrationConfig(io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationConfig mapping) {
        super(mapping);
        id = mapping.id();
        registrationEnabled = mapping.registrationEnabled();
        registerEarly = mapping.registerEarly();
        initialToken = mapping.initialToken();
        metadata.addConfigMappingValues(mapping.metadata());
    }

    /**
     * OIDC Client Registration id
     */
    public Optional<String> id = Optional.empty();

    /**
     * If this client registration configuration is enabled.
     */
    public boolean registrationEnabled = true;

    /**
     * If the client configured with {@link #metadata} must be registered at startup.
     */
    public boolean registerEarly = true;

    /**
     * Initial access token
     */
    public Optional<String> initialToken = Optional.empty();

    /**
     * Client metadata
     */
    public Metadata metadata = new Metadata();

    /**
     * Client metadata
     */
    public static class Metadata {
        /**
         * Client name
         */
        public Optional<String> clientName = Optional.empty();

        /**
         * Redirect URI
         */
        public Optional<String> redirectUri = Optional.empty();

        /**
         * Post Logout URI
         */
        public Optional<String> postLogoutUri = Optional.empty();

        /**
         * Additional metadata properties
         */
        public Map<String, String> extraProps = new HashMap<>();

        private void addConfigMappingValues(
                io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationConfig.Metadata mapping) {
            this.clientName = mapping.clientName();
            this.redirectUri = mapping.redirectUri();
            this.postLogoutUri = mapping.postLogoutUri();
            this.extraProps.putAll(mapping.extraProps());
        }
    }
}
