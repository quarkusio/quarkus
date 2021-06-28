package io.quarkus.io.smallrye.graphql.client;

import java.util.List;

import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi
public interface LuckyNumbersClientApi {

    @Query(value = "get")
    Integer luckyNumber();

    @Mutation(value = "set")
    Integer setLuckyNumber(Integer newLuckyNumber);

    @Query(value = "echoList")
    List<Integer> echoList(@NonNull List<Integer> list);
}
