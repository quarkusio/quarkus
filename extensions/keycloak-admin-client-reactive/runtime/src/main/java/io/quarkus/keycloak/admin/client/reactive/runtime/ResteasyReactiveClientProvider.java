package io.quarkus.keycloak.admin.client.reactive.runtime;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyWriter;

public class ResteasyReactiveClientProvider implements ResteasyClientProvider {

    @Override
    public Client newRestEasyClient(Object messageHandler, SSLContext sslContext, boolean disableTrustManager) {
        ClientBuilderImpl clientBuilder = new ClientBuilderImpl();
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
        }
        return clientBuilder;
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
}
