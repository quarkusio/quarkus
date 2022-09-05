package io.quarkus.keycloak.admin.client.common;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig.GrantType.PASSWORD;

public class KeycloakAdminClientConfigUtil {

    /**
     * Validates configuration properties. KeycloakBuilder also validates inputs (our config properties) when build()
     * is called but that validation is done when the request scoped bean is created and sooner the validation is done, better.
     */
    public static void validate(KeycloakAdminClientConfig config) {

        if (config.serverUrl.isEmpty()) {
            throw new KeycloakAdminClientException("configuration property 'server-url' is required");
        }

        // client id is also required in both cases, but since it's not nullable, we can skip its validation
        if (config.grantType == PASSWORD) {
            if (config.password.isEmpty() || config.username.isEmpty()) {
                throw new KeycloakAdminClientException("grant type 'password' requires username and password");
            }
        } else {
            if (config.clientSecret.isEmpty()) {
                throw new KeycloakAdminClientException("grant type 'client_credentials' requires client secret");
            }
        }
    }

    static final class KeycloakAdminClientException extends RuntimeException {

        private KeycloakAdminClientException(String message) {
            super(String.format("Failed to create Keycloak admin client: %s.", message));
        }

    }
}
