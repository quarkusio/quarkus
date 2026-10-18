package io.quarkus.rest.client.reactive.beanTypes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class MyBean {

    @Inject
    @RestClient
    Client client;

    String test() {
        return client.test();
    }
}
