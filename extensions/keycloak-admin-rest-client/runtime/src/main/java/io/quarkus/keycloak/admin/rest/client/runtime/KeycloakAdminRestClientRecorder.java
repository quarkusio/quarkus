package io.quarkus.keycloak.admin.rest.client.runtime;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

public class KeycloakAdminRestClientRecorder {

    public static void setClientProvider(TlsConfigurationRegistry registry, KeycloakAdminClientConfig config) {
        var namedTlsConfig = TlsConfiguration.from(registry, config.tlsConfigurationName());
        if (namedTlsConfig.isPresent()) {
            Keycloak.setClientProvider(new KeycloakAdminRestClientProvider(namedTlsConfig.get()));
        } else {
            final boolean trustAll;
            if (registry.getDefault().isPresent()) {
                trustAll = registry.getDefault().get().isTrustAll();
            } else {
                trustAll = false;
            }
            Keycloak.setClientProvider(new KeycloakAdminRestClientProvider(trustAll));
        }
    }

    public static Keycloak createAdminClient(KeycloakAdminClientConfig config) {
        if (config.serverUrl().isEmpty()) {
            throw new IllegalStateException(
                    "'quarkus.keycloak.admin-client.server-url' must be set in order to use the Keycloak admin client as a CDI bean");
        }
        return KeycloakBuilder
                .builder()
                .clientId(config.clientId())
                .clientSecret(config.clientSecret().orElse(null))
                .grantType(config.grantType().asString())
                .username(config.username().orElse(null))
                .password(config.password().orElse(null))
                .realm(config.realm())
                .serverUrl(config.serverUrl().get())
                .scope(config.scope().orElse(null))
                .build();
    }

    public static void avoidRuntimeInitIssueInClientBuilderWrapper() {
        // we set our provider at runtime, it is not used before that
        // however org.keycloak.admin.client.Keycloak.CLIENT_PROVIDER is initialized during
        // static init with org.keycloak.admin.client.ClientBuilderWrapper that is not compatible with native mode
        Keycloak.setClientProvider(null);
    }
}
