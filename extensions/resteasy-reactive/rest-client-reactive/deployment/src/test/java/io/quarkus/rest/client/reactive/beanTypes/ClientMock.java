package io.quarkus.rest.client.reactive.beanTypes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.arc.Priority;

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
