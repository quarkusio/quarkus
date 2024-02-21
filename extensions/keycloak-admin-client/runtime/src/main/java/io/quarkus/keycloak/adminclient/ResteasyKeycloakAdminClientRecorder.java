package io.quarkus.keycloak.adminclient;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.spi.ResteasyClientClassicProvider;

import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig;
import io.quarkus.resteasy.common.runtime.jackson.QuarkusJacksonSerializer;
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
            return new Supplier<>() {
                @Override
                public Keycloak get() {
                    throw new IllegalStateException(
                            "'quarkus.keycloak.admin-client.server-url' must be set in order to use the Keycloak admin client as a CDI bean");
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

    public void setClientProvider(boolean tlsTrustAll, boolean areJSONBProvidersPresent) {
        Keycloak.setClientProvider(new ResteasyClientClassicProvider() {
            @Override
            public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
                // point here is to use default Quarkus providers rather than org.keycloak.admin.client.JacksonProvider
                // as it doesn't work properly in native mode
                var builder = ClientBuilderWrapper.create(sslContext, tlsTrustAll || disableTrustManager);
                if (areJSONBProvidersPresent) {
                    // when both Jackson and JSONB providers are present, we need to ensure Jackson is used
                    builder.register(new AppJsonQuarkusJacksonSerializer(), 100);
                }
                return builder.build();
            }
        });
    }

    // makes media type more specific which ensures that it will be used first
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    static class AppJsonQuarkusJacksonSerializer extends QuarkusJacksonSerializer {
    }
}
