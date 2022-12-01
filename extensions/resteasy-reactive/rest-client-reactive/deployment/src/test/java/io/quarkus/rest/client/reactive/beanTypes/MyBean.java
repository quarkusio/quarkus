package io.quarkus.rest.client.reactive.beanTypes;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
