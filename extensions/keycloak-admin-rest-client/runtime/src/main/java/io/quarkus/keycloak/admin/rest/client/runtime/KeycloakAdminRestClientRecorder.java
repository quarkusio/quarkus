package io.quarkus.keycloak.admin.rest.client.runtime;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import tools.jackson.databind.json.JsonMapper;

public class KeycloakAdminRestClientRecorder {

    public static void setClientProvider(TlsConfigurationRegistry registry, KeycloakAdminClientConfig config,
            JsonMapper jsonMapper) {
        var namedTlsConfig = TlsConfiguration.from(registry, config.tlsConfigurationName());
        if (namedTlsConfig.isPresent()) {
            Keycloak.setClientProvider(new KeycloakAdminRestClientProvider(namedTlsConfig.get(), jsonMapper));
        } else {
            final boolean trustAll;
            if (registry.getDefault().isPresent()) {
                trustAll = registry.getDefault().get().isTrustAll();
            } else {
                trustAll = false;
            }
            Keycloak.setClientProvider(new KeycloakAdminRestClientProvider(trustAll, jsonMapper));
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
}
