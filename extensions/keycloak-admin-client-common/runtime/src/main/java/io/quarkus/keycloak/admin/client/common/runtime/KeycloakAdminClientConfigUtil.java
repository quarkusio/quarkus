package io.quarkus.keycloak.admin.client.common.runtime;

import static io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig.GrantType.PASSWORD;

import org.jboss.logging.Logger;

public class KeycloakAdminClientConfigUtil {
    private static final Logger LOG = Logger.getLogger(KeycloakAdminClientConfigUtil.class);

    /**
     * Validates configuration properties. KeycloakBuilder also validates inputs (our config properties) when build()
     * is called but that validation is done when the request scoped bean is created and sooner the validation is done, better.
     */
    public static void validate(KeycloakAdminClientConfig config) {

        if (config.serverUrl().isEmpty()) {
            LOG.debug(
                    "Configuration property 'server-url' is not set, 'Keycloak' admin client injection will fail, "
                            + "use org.keycloak.admin.client.KeycloakBuilder to create it instead");
            return;
        }

        // client id is also required in both cases, but since it's not nullable, we can skip its validation
        if (config.grantType() == PASSWORD) {
            if (config.password().isEmpty() || config.username().isEmpty()) {
                throw new KeycloakAdminClientException("grant type 'password' requires username and password");
            }
        } else {
            if (config.clientSecret().isEmpty()) {
                throw new KeycloakAdminClientException("grant type 'client_credentials' requires client secret");
            }
        }
    }

    public static final class KeycloakAdminClientException extends RuntimeException {

        private KeycloakAdminClientException(String message) {
            super(String.format("Failed to create Keycloak admin client: %s.", message));
        }

    }
}
