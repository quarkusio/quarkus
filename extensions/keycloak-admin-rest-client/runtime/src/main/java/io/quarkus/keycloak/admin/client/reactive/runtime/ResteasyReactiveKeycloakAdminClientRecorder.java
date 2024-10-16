package io.quarkus.keycloak.admin.client.reactive.runtime;

import static io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfigUtil.validate;

import java.util.function.Supplier;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

@Recorder
public class ResteasyReactiveKeycloakAdminClientRecorder {

    private final RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue;

    public ResteasyReactiveKeycloakAdminClientRecorder(
            RuntimeValue<KeycloakAdminClientConfig> keycloakAdminClientConfigRuntimeValue) {
        this.keycloakAdminClientConfigRuntimeValue = keycloakAdminClientConfigRuntimeValue;
    }

    public void setClientProvider(Supplier<TlsConfigurationRegistry> registrySupplier) {
        var registry = registrySupplier.get();
        var namedTlsConfig = TlsConfiguration.from(registry,
                keycloakAdminClientConfigRuntimeValue.getValue().tlsConfigurationName());
        if (namedTlsConfig.isPresent()) {
            Keycloak.setClientProvider(new ResteasyReactiveClientProvider(namedTlsConfig.get()));
        } else {
            final boolean trustAll;
            if (registry.getDefault().isPresent()) {
                trustAll = registry.getDefault().get().isTrustAll();
            } else {
                trustAll = false;
            }
            Keycloak.setClientProvider(new ResteasyReactiveClientProvider(trustAll));
        }
    }

    public Supplier<Keycloak> createAdminClient() {

        final KeycloakAdminClientConfig config = keycloakAdminClientConfigRuntimeValue.getValue();
        validate(config);
        if (config.serverUrl().isEmpty()) {
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
                .clientId(config.clientId())
                .clientSecret(config.clientSecret().orElse(null))
                .grantType(config.grantType().asString())
                .username(config.username().orElse(null))
                .password(config.password().orElse(null))
                .realm(config.realm())
                .serverUrl(config.serverUrl().get())
                .scope(config.scope().orElse(null));
        return new Supplier<Keycloak>() {
            @Override
            public Keycloak get() {
                return keycloakBuilder.build();
            }
        };
    }

    public void avoidRuntimeInitIssueInClientBuilderWrapper() {
        // we set our provider at runtime, it is not used before that
        // however org.keycloak.admin.client.Keycloak.CLIENT_PROVIDER is initialized during
        // static init with org.keycloak.admin.client.ClientBuilderWrapper that is not compatible with native mode
        Keycloak.setClientProvider(null);
    }
}
