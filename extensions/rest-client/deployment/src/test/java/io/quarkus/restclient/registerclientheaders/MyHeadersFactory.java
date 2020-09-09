package io.quarkus.restclient.registerclientheaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

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
