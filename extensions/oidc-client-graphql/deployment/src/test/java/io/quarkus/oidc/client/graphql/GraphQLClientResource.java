package io.quarkus.oidc.client.graphql;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

@Path("/oidc-graphql-client")
public class GraphQLClientResource {

    @Inject
    AnnotationTypesafeGraphQLClient annotationTypesafeClient;

    @Inject
    DefaultTypesafeGraphQLClient defaultTypesafeClient;

    @GET
    @Path("/annotation-typesafe")
    public String typesafeClientAnnotation() {
        return annotationTypesafeClient.principalName();
    }

    @GET
    @Path("/default-typesafe")
    public String typesafeClientDefault() {
        return defaultTypesafeClient.principalName();
    }

    @Inject
    @GraphQLClient("default-dynamic")
    DynamicGraphQLClient dynamicClient;

    @GET
    @Path("/default-dynamic")
    public String dynamicClientDefault() throws ExecutionException, InterruptedException {
        return dynamicClient.executeSync("query { principalName }").getData().getString("principalName");
    }

}
