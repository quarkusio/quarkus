package io.quarkus.reactive.mssql.client;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.reactive.datasource.ChangingCredentialsProviderBase;

@ApplicationScoped
public class ChangingCredentialsProvider extends ChangingCredentialsProviderBase {

    public ChangingCredentialsProvider() {
        super("sa", "yourStrong(!)Password", "user2", "yourStrong(!)Password2");
    }
}
