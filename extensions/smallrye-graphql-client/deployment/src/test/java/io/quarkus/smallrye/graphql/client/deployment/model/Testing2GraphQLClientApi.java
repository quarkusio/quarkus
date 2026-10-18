package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;

import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "secondtypesafeclient", endpoint = "https://graphql.acme.org/example")
public interface Testing2GraphQLClientApi {

    @Query
    public List<Person> people();

}
