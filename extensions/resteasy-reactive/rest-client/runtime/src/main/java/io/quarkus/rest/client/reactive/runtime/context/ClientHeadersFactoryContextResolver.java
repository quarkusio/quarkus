package io.quarkus.rest.client.reactive.runtime.context;

import jakarta.ws.rs.ext.ContextResolver;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class ClientHeadersFactoryContextResolver implements ContextResolver<ClientHeadersFactory> {

    private final ClientHeadersFactory component;

    public ClientHeadersFactoryContextResolver(ClientHeadersFactory component) {
        this.component = component;
    }

    @Override
    public ClientHeadersFactory getContext(Class<?> wantedClass) {
        if (wantedClass.equals(ClientHeadersFactory.class)) {
            return component;
        }

        return null;
    }
}
