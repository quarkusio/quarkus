package io.quarkus.smallrye.graphql.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that is used when no endpoint is available
 */
public class SmallRyeGraphQLNoEndpointHandler implements Handler<RoutingContext> {
    private static final String CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String MESSAGE = "GraphQL Schema not generated. Make sure you have a GraphQL Endpoint. Go to https://quarkus.io/guides/microprofile-graphql to learn how";

    @Override
    public void handle(RoutingContext event) {
        HttpServerResponse response = event.response();
        response.headers().set(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        response.setStatusCode(404).end(MESSAGE);
    }
}
