package io.quarkus.rest.client.reactive.beanTypes;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Alternative()
@Priority(1)
@ApplicationScoped
@RestClient
public class ClientMock implements Client, Charlie {

    @Override
    public String test() {
        return "hello from " + ClientMock.class;
    }
}
