package io.quarkus.keycloak.admin.rest.client.runtime;

import java.security.KeyStore;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.TlsConfig;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyWriter;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;
import tools.jackson.databind.json.JsonMapper;

public class KeycloakAdminRestClientProvider implements ResteasyClientProvider {

    private static final List<String> HANDLED_MEDIA_TYPES = List.of(MediaType.APPLICATION_JSON);
    private static final int WRITER_PROVIDER_PRIORITY = Priorities.USER + 100; // ensures that it will be used first
    private static final int READER_PROVIDER_PRIORITY = Priorities.USER - 100; // ensures that it will be used first

    private final boolean tlsTrustAll;
    private final TlsConfig tlsConfig;
    // the mapper selected at build time for the Keycloak representations; this ensures we don't customize
    // the managed (shared) ObjectMapper available in the CDI container
    private final JsonMapper jsonMapper;

    public KeycloakAdminRestClientProvider(boolean tlsTrustAll, JsonMapper jsonMapper) {
        this.tlsTrustAll = tlsTrustAll;
        this.tlsConfig = null;
        this.jsonMapper = jsonMapper;
    }

    public KeycloakAdminRestClientProvider(TlsConfiguration tlsConfiguration, JsonMapper jsonMapper) {
        this.tlsTrustAll = tlsConfiguration.isTrustAll();
        this.tlsConfig = createTlsConfig(tlsConfiguration);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Client newRestEasyClient(Object messageHandler, SSLContext sslContext, boolean disableTrustManager) {
        ClientBuilderImpl clientBuilder = new ClientBuilderImpl();
        if (tlsConfig == null) {
            clientBuilder.trustAll(tlsTrustAll || disableTrustManager);
        } else {
            clientBuilder.tlsConfig(tlsConfig);
        }
        return registerJacksonProviders(clientBuilder).build();
    }

    // this code is much more complicated than expected because it needs to handle various permutations
    // where beans may or may not exist
    private ClientBuilderImpl registerJacksonProviders(ClientBuilderImpl clientBuilder) {
        ArcContainer arcContainer = Arc.container();
        if (arcContainer == null) {
            throw new IllegalStateException(this.getClass().getName() + " should only be used in a Quarkus application");
        } else {
            clientBuilder = clientBuilder
                    .registerMessageBodyReader(new JacksonBasicMessageBodyReader(jsonMapper), Object.class,
                            HANDLED_MEDIA_TYPES, true,
                            READER_PROVIDER_PRIORITY)
                    .registerMessageBodyWriter(new ClientJacksonMessageBodyWriter(jsonMapper), Object.class,
                            HANDLED_MEDIA_TYPES, true, WRITER_PROVIDER_PRIORITY);
            InstanceHandle<ClientLogger> clientLogger = arcContainer.instance(ClientLogger.class);
            if (clientLogger.isAvailable()) {
                clientBuilder.clientLogger(clientLogger.get());
            }
        }
        return clientBuilder;
    }

    @Override
    public <R> R targetProxy(WebTarget target, Class<R> targetClass) {
        return ((WebTargetImpl) target).proxy(targetClass);
    }

    private static TlsConfig createTlsConfig(TlsConfiguration tlsConfiguration) {
        return new TlsConfig() {
            @Override
            public KeyStore getKeyStore() {
                return tlsConfiguration.getKeyStore();
            }

            @Override
            public KeyCertOptions getKeyStoreOptions() {
                return tlsConfiguration.getKeyStoreOptions();
            }

            @Override
            public KeyStore getTrustStore() {
                return tlsConfiguration.getTrustStore();
            }

            @Override
            public TrustOptions getTrustStoreOptions() {
                return tlsConfiguration.getTrustStoreOptions();
            }

            @Override
            public SSLOptions getSSLOptions() {
                return tlsConfiguration.getClientSSLOptions();
            }

            @Override
            public SSLContext createSSLContext() throws Exception {
                return tlsConfiguration.createSSLContext();
            }

            @Override
            public Optional<String> getHostnameVerificationAlgorithm() {
                return tlsConfiguration.getHostnameVerificationAlgorithm();
            }

            @Override
            public boolean usesSni() {
                return tlsConfiguration.usesSni();
            }

            @Override
            public boolean isTrustAll() {
                return tlsConfiguration.isTrustAll();
            }

            @Override
            public Optional<String> getName() {
                return Optional.ofNullable(tlsConfiguration.getName());
            }
        };
    }
}
