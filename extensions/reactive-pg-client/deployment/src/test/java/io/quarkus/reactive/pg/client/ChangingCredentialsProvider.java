package io.quarkus.reactive.pg.client;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.reactive.datasource.ChangingCredentialsProviderBase;

@ApplicationScoped
public class ChangingCredentialsProvider extends ChangingCredentialsProviderBase {

    public ChangingCredentialsProvider() {
        super("hibernate_orm_test", "hibernate_orm_test", "user2", "user2");
    }
}
