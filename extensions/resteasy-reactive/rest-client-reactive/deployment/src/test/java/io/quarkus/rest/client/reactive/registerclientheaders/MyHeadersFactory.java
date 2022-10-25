package io.quarkus.rest.client.reactive.registerclientheaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@Singleton
public class MyHeadersFactory implements ClientHeadersFactory {

    @Inject
    BeanManager beanManager;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        assertNotNull(beanManager);
        incomingHeaders.add("foo", "bar");
        return incomingHeaders;
    }

}
