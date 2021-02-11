package io.quarkus.it.rest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

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
