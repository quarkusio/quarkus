package io.quarkus.keycloak.adminclient;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;

import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.spi.ResteasyClientClassicProvider;

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

    public void setClientProvider() {
        Keycloak.setClientProvider(new ResteasyClientClassicProvider() {
            @Override
            public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
                // point here is to use default Quarkus providers rather than org.keycloak.admin.client.JacksonProvider
                // as it doesn't work properly in native mode
                return ClientBuilderWrapper.create(sslContext, disableTrustManager).build();
            }
        });
    }
}
