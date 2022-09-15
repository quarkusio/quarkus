package io.quarkus.it.rest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@ApplicationScoped
public class ClientProducer {
    private Client client;

    @PostConstruct
    void init() {
        client = ClientBuilder.newClient().register(LoggingFilter.class);
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }

    @Produces
    @ApplicationScoped
    public Client getClient() {
        return client;
    }
}
