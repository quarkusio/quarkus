/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.keycloak.admin.client.reactive;

import java.net.URI;

import javax.ws.rs.client.Client;

import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.keycloak.admin.client.reactive.resource.BearerAuthFilter;
import io.quarkus.keycloak.admin.client.reactive.resource.RealmResource;
import io.quarkus.keycloak.admin.client.reactive.resource.RealmsResource;
import io.quarkus.keycloak.admin.client.reactive.resource.ServerInfoResource;
import io.quarkus.keycloak.admin.client.reactive.token.TokenManager;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyWriter;

/**
 * Provides a Keycloak client.
 * To customize the underling client, use a {@link KeycloakBuilder} to create a Keycloak client.
 *
 * @author rodrigo.sasaki@icarros.com.br
 * @see KeycloakBuilder
 */
public class Keycloak implements AutoCloseable {
    private final TokenManager tokenManager;
    private final String authToken;
    private final WebTargetImpl target;
    private final Client client;
    private volatile boolean closed = false;

    Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret,
            String grantType, String authtoken) {
        Config config = new Config(serverUrl, realm, username, password, clientId, clientSecret, grantType);
        ClientBuilderImpl clientBuilder = new ClientBuilderImpl();
        clientBuilder = registerJacksonProviders(clientBuilder);
        client = clientBuilder.build();
        authToken = authtoken;
        tokenManager = authtoken == null ? new TokenManager(config, client) : null;

        target = (WebTargetImpl) client.target(config.getServerUrl());
        target.register(newAuthFilter());
    }

    // this code is much more complicated than expected because it needs to handle various permutations
    // where beans may or may not exist
    private ClientBuilderImpl registerJacksonProviders(ClientBuilderImpl clientBuilder) {
        ArcContainer arcContainer = Arc.container();
        if (arcContainer == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            clientBuilder = clientBuilder
                    .register(new JacksonBasicMessageBodyReader(objectMapper))
                    .register(new ClientJacksonMessageBodyWriter(objectMapper));
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

    private BearerAuthFilter newAuthFilter() {
        return authToken != null ? new BearerAuthFilter(authToken) : new BearerAuthFilter(tokenManager);
    }

    public RealmsResource realms() {
        return target.proxy(RealmsResource.class);
    }

    public RealmResource realm(String realmName) {
        return realms().realm(realmName);
    }

    public ServerInfoResource serverInfo() {
        return target.proxy(ServerInfoResource.class);
    }

    public TokenManager tokenManager() {
        return tokenManager;
    }

    /**
     * Create a secure proxy based on an absolute URI.
     * All set up with appropriate token
     *
     * @param proxyClass
     * @param absoluteURI
     * @param <T>
     * @return
     */
    public <T> T proxy(Class<T> proxyClass, URI absoluteURI) {
        return ((WebTargetImpl) client.target(absoluteURI)).register(newAuthFilter()).proxy(proxyClass);
    }

    /**
     * Closes the underlying client. After calling this method, this <code>Keycloak</code> instance cannot be reused.
     */
    @Override
    public void close() {
        closed = true;
        client.close();
    }

    /**
     * @return true if the underlying client is closed.
     */
    public boolean isClosed() {
        return closed;
    }
}
