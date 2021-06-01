package io.quarkus.io.smallrye.graphql.client;

import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi
public interface LuckyNumbersClientApi {

    @Query(value = "get")
    Integer luckyNumber();

    @Mutation(value = "set")
    Integer setLuckyNumber(Integer newLuckyNumber);
}
