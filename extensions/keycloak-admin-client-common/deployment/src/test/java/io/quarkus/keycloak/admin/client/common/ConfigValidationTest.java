package io.quarkus.keycloak.admin.client.common;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigValidationTest {

    @Test
    public void passwordGrantTypeTest() {
        // negative test
        Assertions.assertDoesNotThrow(() -> validate(createConfig()));

        // username is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            KeycloakAdminClientConfig config = createConfig();
            config.username = Optional.empty();
            validate(config);
        });

        // password is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            KeycloakAdminClientConfig config = createConfig();
            config.password = Optional.empty();
            validate(config);
        });
    }

    @Test
    public void clientCredentialsGrantTypeTest() {
        // negative test
        Assertions.assertDoesNotThrow(() -> validate(createClientCredentialsConfig()));

        // client secret is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            KeycloakAdminClientConfig config = createClientCredentialsConfig();
            config.clientSecret = Optional.empty();
            validate(config);
        });
    }

    private KeycloakAdminClientConfig createConfig() {
        final KeycloakAdminClientConfig config = new KeycloakAdminClientConfig();
        config.serverUrl = Optional.of("https://localhost:8081");
        config.grantType = KeycloakAdminClientConfig.GrantType.PASSWORD;
        config.clientId = "client id";
        config.clientSecret = Optional.empty();
        config.username = Optional.of("john");
        config.password = Optional.of("pwd");
        config.realm = "master";
        config.scope = Optional.empty();
        return config;
    }

    private KeycloakAdminClientConfig createClientCredentialsConfig() {
        final KeycloakAdminClientConfig config = createConfig();
        config.grantType = KeycloakAdminClientConfig.GrantType.CLIENT_CREDENTIALS;
        config.password = Optional.empty();
        config.username = Optional.empty();
        config.clientSecret = Optional.of("client secret");
        return config;
    }

}
