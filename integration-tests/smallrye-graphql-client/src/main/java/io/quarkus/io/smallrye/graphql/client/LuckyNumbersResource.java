package io.quarkus.io.smallrye.graphql.client;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;

@GraphQLApi
@ApplicationScoped
public class LuckyNumbersResource {

    private volatile Integer luckyNumber = 12;

    @Query(value = "get")
    public Integer luckyNumber() {
        return luckyNumber;
    }

    @Mutation(value = "set")
    public Integer setLuckyNumber(Integer newLuckyNumber) {
        luckyNumber = newLuckyNumber;
        return luckyNumber;
    }

    @Subscription
    public Multi<Integer> primeNumbers() {
        return Multi.createFrom().items(2, 3, 5, 7, 11, 13);
    }

    @Query(value = "echoList")
    public List<Integer> echoList(@NonNull List<Integer> list) {
        return list;
    }

}
