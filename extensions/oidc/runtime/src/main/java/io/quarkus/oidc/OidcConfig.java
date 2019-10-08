package io.quarkus.oidc;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcConfig {

    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, 'https://host:port/auth'.
     * All the other OIDC server page and service URLs are derived from this URL.
     * Note if you work with Keycloak OIDC server, make sure the base URL is in the following format:
     * 'https://host:port/auth/realms/{realm}' where '{realm}' has to be replaced by the name of the Keycloak realm.
     */
    @ConfigItem
    String authServerUrl;

    /**
     * Relative path of the RFC7662 introspection service address.
     * Default value is currently set to the path supported by Keycloak.
     */
    @ConfigItem(defaultValue = "/protocol/openid-connect/token/introspect")
    String introspectionPath;

    /**
     * Public key for the local JWT token verification.
     */
    @ConfigItem
    Optional<String> publicKey;

    /**
     * The client-id of the application. Each application has a client-id that is used to identify the application
     */
    @ConfigItem
    Optional<String> clientId;

    /**
     * Credentials which the OIDC adapter will use to authenticate to the OIDC server.
     */
    @ConfigItem
    Credentials credentials;

    @ConfigGroup
    public static class Credentials {

        /**
         * The client secret
         */
        @ConfigItem
        Optional<String> secret;

    }

}
