package io.quarkus.reactive.mssql.client;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.reactive.datasource.ChangingCredentialsProviderBase;

@ApplicationScoped
public class ChangingCredentialsProvider extends ChangingCredentialsProviderBase {

    public ChangingCredentialsProvider() {
        super("sa", "A_Str0ng_Required_Password", "user2", "user2_Has_A_Str0ng_Required_Password");
    }
}
