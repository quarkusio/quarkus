package io.quarkus.keycloak.admin.rest.client.runtime;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

/**
 * Loaded by Keycloak through the {@link java.util.ServiceLoader} when the {@link Keycloak} class is initialized, which
 * requires a provider (the one of Keycloak needs RESTEasy Classic). It is replaced by the configured
 * {@link KeycloakAdminRestClientProvider} at startup, see {@link KeycloakAdminRestClientRecorder#setClientProvider}.
 */
public final class KeycloakAdminRestClientProviderPlaceholder implements ResteasyClientProvider {

    @Override
    public Client newRestEasyClient(Object messageHandler, SSLContext sslContext, boolean disableTrustManager) {
        throw notConfigured();
    }

    @Override
    public <R> R targetProxy(WebTarget target, Class<R> targetClass) {
        throw notConfigured();
    }

    private static IllegalStateException notConfigured() {
        return new IllegalStateException(
                "The Keycloak admin client provider is not configured yet, it is set when the application starts");
    }
}
