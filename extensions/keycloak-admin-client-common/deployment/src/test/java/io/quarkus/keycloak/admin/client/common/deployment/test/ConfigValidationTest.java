package io.quarkus.keycloak.admin.client.common.deployment.test;

import static io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfigUtil.validate;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfigUtil;
import io.smallrye.config.ConfigInstanceBuilder;

public class ConfigValidationTest {

    @Test
    public void passwordGrantTypeTest() {
        // negative test
        Assertions.assertDoesNotThrow(() -> validate(createConfig().build()));

        // username is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            validate(createConfig().with(KeycloakAdminClientConfig::username, Optional.<String> empty()).build());
        });

        // password is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            validate(createConfig().with(KeycloakAdminClientConfig::password, Optional.<String> empty()).build());
        });
    }

    @Test
    public void clientCredentialsGrantTypeTest() {
        // negative test
        Assertions.assertDoesNotThrow(() -> validate(createClientCredentialsConfig().build()));

        // client secret is required
        Assertions.assertThrows(KeycloakAdminClientConfigUtil.KeycloakAdminClientException.class, () -> {
            validate(createClientCredentialsConfig().with(KeycloakAdminClientConfig::clientSecret, Optional.<String> empty())
                    .build());
        });
    }

    private ConfigInstanceBuilder<KeycloakAdminClientConfig> createConfig() {
        return ConfigInstanceBuilder.forInterface(KeycloakAdminClientConfig.class)
                .withOptional(KeycloakAdminClientConfig::serverUrl, "https://localhost:8081")
                .with(KeycloakAdminClientConfig::grantType, KeycloakAdminClientConfig.GrantType.PASSWORD)
                .with(KeycloakAdminClientConfig::clientId, "client id")
                .withOptional(KeycloakAdminClientConfig::username, "john")
                .withOptional(KeycloakAdminClientConfig::password, "pwd")
                .with(KeycloakAdminClientConfig::realm, "master");
    }

    private ConfigInstanceBuilder<KeycloakAdminClientConfig> createClientCredentialsConfig() {
        return createConfig()
                .with(KeycloakAdminClientConfig::grantType, KeycloakAdminClientConfig.GrantType.CLIENT_CREDENTIALS)
                .with(KeycloakAdminClientConfig::password, Optional.<String> empty())
                .with(KeycloakAdminClientConfig::username, Optional.<String> empty())
                .withOptional(KeycloakAdminClientConfig::clientSecret, "client secret");
    }
}
