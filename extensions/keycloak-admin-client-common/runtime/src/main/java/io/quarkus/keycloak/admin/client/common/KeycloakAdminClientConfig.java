package io.quarkus.keycloak.admin.client.common;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Keycloak Admin Client
 */
@ConfigMapping(prefix = "quarkus.keycloak.admin-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface KeycloakAdminClientConfig {

    /**
     * Keycloak server URL, for example, `https://host:port`.
     * If this property is not set then the Keycloak Admin Client injection will fail - use
     * {@linkplain org.keycloak.admin.client.KeycloakBuilder}
     * to create it instead.
     */
    Optional<String> serverUrl();

    /**
     * Realm.
     */
    @WithDefault("master")
    String realm();

    /**
     * Client id.
     */
    @WithDefault("admin-cli")
    String clientId();

    /**
     * Client secret. Required with a `client_credentials` grant type.
     */
    Optional<String> clientSecret();

    /**
     * Username. Required with a `password` grant type.
     */
    @WithDefault("admin")
    Optional<String> username();

    /**
     * Password. Required with a `password` grant type.
     */
    @WithDefault("admin")
    Optional<String> password();

    /**
     * OAuth 2.0 <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-3.3">Access Token Scope</a>.
     */
    Optional<String> scope();

    /**
     * OAuth Grant Type.
     */
    @WithDefault("PASSWORD")
    GrantType grantType();

    enum GrantType {
        PASSWORD,
        CLIENT_CREDENTIALS;

        public String asString() {
            return this.toString().toLowerCase();
        }
    }

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * The default TLS configuration is <strong>not</strong> used by default.
     */
    Optional<String> tlsConfigurationName();

}
