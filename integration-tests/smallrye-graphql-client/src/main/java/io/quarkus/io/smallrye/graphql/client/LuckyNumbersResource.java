package io.quarkus.io.smallrye.graphql.client;

import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import graphql.execution.directives.QueryDirectives;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.mutiny.Multi;

@GraphQLApi
public class LuckyNumbersResource {

    private volatile Integer luckyNumber = 12;

    @Inject
    CurrentVertxRequest request;

    @Inject
    SmallRyeContext context;

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

    @Query
    public String returnHeader(String key) {
        return request.getCurrent().request().getHeader(key);
    }

    @Query
    public String piNumber() {
        QueryDirectives directives = context.getDataFetchingEnvironment().getQueryDirectives();
        if (directives.getImmediateAppliedDirective("skip").isEmpty()) {
            throw new IllegalArgumentException("Directive 'skip' was not found in the query (on the server side).");
        }

        return "3.14159";
    }

}
