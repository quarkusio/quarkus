package io.quarkus.keycloak.admin.resteasy.client.runtime;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.jackson3.JacksonProvider3;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

import io.quarkus.tls.TlsConfiguration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Replaces the default provider of Keycloak at startup, see {@link KeycloakAdminResteasyClientRecorder#setClientProvider}.
 */
public class KeycloakAdminResteasyClientProvider implements ResteasyClientProvider {

    private final TlsConfiguration namedTlsConfig;
    private final boolean globalTrustAll;
    private final JsonMapper jsonMapper;

    public KeycloakAdminResteasyClientProvider(TlsConfiguration namedTlsConfig, boolean globalTrustAll,
            JsonMapper jsonMapper) {
        this.namedTlsConfig = namedTlsConfig;
        this.globalTrustAll = globalTrustAll;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
        // this is what 'org.keycloak.admin.client.jackson3.ResteasyClientClassicProvider3' does,
        // except for the TLS configuration coming from the TLS registry
        var builder = new ResteasyClientBuilderImpl();
        builder.connectionPoolSize(10);

        if (namedTlsConfig == null) {
            builder.sslContext(sslContext);
            if (globalTrustAll || disableTrustManager) {
                builder.disableTrustManager();
            }
        } else {
            if (namedTlsConfig.isTrustAll()) {
                builder.disableTrustManager();
            }
            try {
                builder.sslContext(namedTlsConfig.createSSLContext());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create Keycloak Admin client SSLContext", e);
            }
        }

        // the Jackson provider of the Keycloak admin client with its own mapper, registered with the Keycloak priority
        // so that it is used instead of the JSON providers of the Quarkus RESTEasy client; this ensures we don't
        // customize the managed (shared) ObjectMapper available in the CDI container
        Object provider = customJacksonProvider != null ? customJacksonProvider : jacksonProvider();
        builder.register(provider, 100);

        return builder.build();
    }

    @Override
    public <R> R targetProxy(WebTarget webTarget, Class<R> aClass) {
        return (ResteasyWebTarget.class.cast(webTarget)).proxy(aClass);
    }

    private JacksonProvider3 jacksonProvider() {
        JacksonProvider3 provider = new JacksonProvider3();
        provider.setMapper(jsonMapper);
        return provider;
    }
}
