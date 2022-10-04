package io.quarkus.keycloak.adminclient;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.function.Supplier;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyKeycloakAdminClientRecorder {

    private final RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue;

    public ResteasyKeycloakAdminClientRecorder(
            RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue) {
        this.keycloakAdminClientConfigRuntimeValue = keycloakAdminClientConfigRuntimeValue;
    }

    public Supplier<Keycloak> createAdminClient() {

        final KeycloakAdminClientConfig config = keycloakAdminClientConfigRuntimeValue.getValue();
        validate(config);
        if (config.serverUrl.isEmpty()) {
            return new Supplier<Keycloak>() {
                @Override
                public Keycloak get() {
                    return null;
                }
            };
        }
        final KeycloakBuilder keycloakBuilder = KeycloakBuilder
                .builder()
                .clientId(config.clientId)
                .clientSecret(config.clientSecret.orElse(null))
                .grantType(config.grantType.asString())
                .username(config.username.orElse(null))
                .password(config.password.orElse(null))
                .realm(config.realm)
                .serverUrl(config.serverUrl.get())
                .scope(config.scope.orElse(null));
        return new Supplier<Keycloak>() {
            @Override
            public Keycloak get() {
                return keycloakBuilder.build();
            }
        };
    }
}
