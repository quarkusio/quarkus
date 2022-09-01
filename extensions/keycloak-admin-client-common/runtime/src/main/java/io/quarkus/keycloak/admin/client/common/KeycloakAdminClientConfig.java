package io.quarkus.keycloak.admin.client.common;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Keycloak Admin Client
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "keycloak.admin-client")
public class KeycloakAdminClientConfig {

    /**
     * Realm.
     */
    @ConfigItem(defaultValue = "master")
    public String realm;

    /**
     * Keycloak server URL, for example, `https://host:port`.
     */
    @ConfigItem
    public Optional<String> serverUrl;

    /**
     * Client id.
     */
    @ConfigItem(defaultValue = "admin-cli")
    public String clientId;

    /**
     * Client secret. Required with a `client_credentials` grant type.
     */
    @ConfigItem
    public Optional<String> clientSecret;

    /**
     * Username. Required with a `password` grant type.
     */
    @ConfigItem(defaultValue = "admin")
    public Optional<String> username;

    /**
     * Password. Required with a `password` grant type.
     */
    @ConfigItem(defaultValue = "admin")
    public Optional<String> password;

    /**
     * OAuth 2.0 <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-3.3">Access Token Scope</a>.
     */
    @ConfigItem
    public Optional<String> scope;

    /**
     * OAuth Grant Type.
     */
    @ConfigItem(defaultValue = "PASSWORD")
    public GrantType grantType;

    public enum GrantType {
        PASSWORD,
        CLIENT_CREDENTIALS;

        public String asString() {
            return this.toString().toLowerCase();
        }
    }

}
