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
            KeycloakAdminClientConfigImpl config = createConfig();
            config.username = Optional.empty();
            validate(config);
        });

        // password is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            KeycloakAdminClientConfigImpl config = createConfig();
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
            KeycloakAdminClientConfigImpl config = createClientCredentialsConfig();
            config.clientSecret = Optional.empty();
            validate(config);
        });
    }

    private KeycloakAdminClientConfigImpl createConfig() {
        final KeycloakAdminClientConfigImpl config = new KeycloakAdminClientConfigImpl();
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

    private KeycloakAdminClientConfigImpl createClientCredentialsConfig() {
        final KeycloakAdminClientConfigImpl config = createConfig();
        config.grantType = KeycloakAdminClientConfig.GrantType.CLIENT_CREDENTIALS;
        config.password = Optional.empty();
        config.username = Optional.empty();
        config.clientSecret = Optional.of("client secret");
        return config;
    }

    private static final class KeycloakAdminClientConfigImpl implements KeycloakAdminClientConfig {

        private Optional<String> password;
        private Optional<String> username;
        private Optional<String> clientSecret;
        private Optional<String> scope;
        private Optional<String> serverUrl;
        private String realm;
        private String clientId;
        private KeycloakAdminClientConfig.GrantType grantType;

        @Override
        public Optional<String> serverUrl() {
            return serverUrl;
        }

        @Override
        public String realm() {
            return realm;
        }

        @Override
        public String clientId() {
            return clientId;
        }

        @Override
        public Optional<String> clientSecret() {
            return clientSecret;
        }

        @Override
        public Optional<String> username() {
            return username;
        }

        @Override
        public Optional<String> password() {
            return password;
        }

        @Override
        public Optional<String> scope() {
            return scope;
        }

        @Override
        public GrantType grantType() {
            return grantType;
        }

        @Override
        public Optional<String> tlsConfigurationName() {
            return Optional.empty();
        }
    }

}
