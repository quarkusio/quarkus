package io.quarkus.keycloak.admin.resteasy.client.runtime;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import tools.jackson.databind.json.JsonMapper;

public class KeycloakAdminResteasyClientRecorder {

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

    public static void setClientProvider(TlsConfigurationRegistry registry, KeycloakAdminClientConfig config,
            JsonMapper jsonMapper) {
        var namedTlsConfig = TlsConfiguration.from(registry, config.tlsConfigurationName()).orElse(null);
        final boolean globalTrustAll;
        if (registry.getDefault().isPresent()) {
            globalTrustAll = registry.getDefault().get().isTrustAll();
        } else {
            globalTrustAll = false;
        }
        Keycloak.setClientProvider(new KeycloakAdminResteasyClientProvider(namedTlsConfig, globalTrustAll, jsonMapper));
    }
}
