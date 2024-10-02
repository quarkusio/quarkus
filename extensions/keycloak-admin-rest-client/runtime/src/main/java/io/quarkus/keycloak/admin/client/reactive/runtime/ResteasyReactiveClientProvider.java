package io.quarkus.keycloak.admin.client.reactive.runtime;

import java.security.KeyStore;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.client.TlsConfig;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyWriter;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

public class ResteasyReactiveClientProvider implements ResteasyClientProvider {

    private static final List<String> HANDLED_MEDIA_TYPES = List.of(MediaType.APPLICATION_JSON);
    private static final int WRITER_PROVIDER_PRIORITY = Priorities.USER + 100; // ensures that it will be used first
    private static final int READER_PROVIDER_PRIORITY = Priorities.USER - 100; // ensures that it will be used first

    private final boolean tlsTrustAll;
    private final TlsConfig tlsConfig;

    public ResteasyReactiveClientProvider(boolean tlsTrustAll) {
        this.tlsTrustAll = tlsTrustAll;
        this.tlsConfig = null;
    }

    public ResteasyReactiveClientProvider(TlsConfiguration tlsConfiguration) {
        tlsTrustAll = tlsConfiguration.isTrustAll();
        this.tlsConfig = createTlsConfig(tlsConfiguration);
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
            InstanceHandle<ObjectMapper> objectMapperInstance = arcContainer.instance(ObjectMapper.class);
            boolean canReuseObjectMapper = canReuseObjectMapper(objectMapperInstance, arcContainer);
            if (canReuseObjectMapper) {

                ObjectMapper objectMapper = null;

                InstanceHandle<JacksonBasicMessageBodyReader> readerInstance = arcContainer
                        .instance(JacksonBasicMessageBodyReader.class);
                if (readerInstance.isAvailable()) {
                    clientBuilder = clientBuilder.register(readerInstance.get());
                } else {
                    objectMapper = getObjectMapper(objectMapper, objectMapperInstance);
                    clientBuilder = clientBuilder.register(new JacksonBasicMessageBodyReader(objectMapper));
                }

                InstanceHandle<ClientJacksonMessageBodyWriter> writerInstance = arcContainer
                        .instance(ClientJacksonMessageBodyWriter.class);
                if (writerInstance.isAvailable()) {
                    clientBuilder = clientBuilder.register(writerInstance.get());
                } else {
                    objectMapper = getObjectMapper(objectMapper, objectMapperInstance);
                    clientBuilder = clientBuilder.register(new ClientJacksonMessageBodyWriter(objectMapper));
                }
            } else {
                ObjectMapper newObjectMapper = new ObjectMapper();
                clientBuilder = clientBuilder
                        .registerMessageBodyReader(new JacksonBasicMessageBodyReader(newObjectMapper), Object.class,
                                HANDLED_MEDIA_TYPES, true,
                                READER_PROVIDER_PRIORITY)
                        .registerMessageBodyWriter(new ClientJacksonMessageBodyWriter(newObjectMapper), Object.class,
                                HANDLED_MEDIA_TYPES, true, WRITER_PROVIDER_PRIORITY);
            }
            InstanceHandle<ClientLogger> clientLogger = arcContainer.instance(ClientLogger.class);
            if (clientLogger.isAvailable()) {
                clientBuilder.clientLogger(clientLogger.get());
            }
        }
        return clientBuilder;
    }

    // the idea is to only reuse the ObjectMapper if no known customizations would break Keycloak
    // TODO: in the future we could also look into checking the ObjectMapper bean itself to see how it has been configured
    private boolean canReuseObjectMapper(InstanceHandle<ObjectMapper> objectMapperInstance, ArcContainer arcContainer) {
        if (objectMapperInstance.isAvailable() && !objectMapperInstance.getBean().isDefaultBean()) {
            // in this case a user provided a completely custom ObjectMapper, so we can't use it
            return false;
        }

        Instance<ObjectMapperCustomizer> customizers = arcContainer.beanManager().createInstance()
                .select(ObjectMapperCustomizer.class);
        if (!customizers.isUnsatisfied()) {
            // ObjectMapperCustomizer can make arbitrary changes, so in order to be safe we won't allow reuse
            return false;
        }
        // if any Jackson properties were configured, disallow reuse - this is done in order to provide forward compatibility with new Jackson configuration options
        for (String propertyName : ConfigProvider.getConfig().getPropertyNames()) {
            if (propertyName.startsWith("quarkus.jackson")) {
                return false;
            }
        }
        return true;
    }

    // the whole idea here is to reuse the ObjectMapper instance
    private ObjectMapper getObjectMapper(ObjectMapper value,
            InstanceHandle<ObjectMapper> objectMapperInstance) {
        if (value == null) {
            return objectMapperInstance.isAvailable() ? objectMapperInstance.get() : new ObjectMapper();
        }
        return value;
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
                return tlsConfiguration.getSSLOptions();
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
        };
    }
}
