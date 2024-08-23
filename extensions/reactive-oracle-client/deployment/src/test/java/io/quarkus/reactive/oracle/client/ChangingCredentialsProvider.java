package io.quarkus.reactive.oracle.client;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.reactive.datasource.ChangingCredentialsProviderBase;

@ApplicationScoped
public class ChangingCredentialsProvider extends ChangingCredentialsProviderBase {

    public ChangingCredentialsProvider() {
        super("SYSTEM", "hibernate_orm_test", "user2", "user2");
    }
}
