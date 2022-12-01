package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;

import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "typesafeclient")
public interface TestingGraphQLClientApi {

    @Query
    public List<Person> people();

    @Query
    public String returnHeader(String key);

}
