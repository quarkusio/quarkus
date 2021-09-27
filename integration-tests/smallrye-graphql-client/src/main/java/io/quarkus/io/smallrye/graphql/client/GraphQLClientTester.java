package io.quarkus.io.smallrye.graphql.client;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Argument.args;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.core.OperationType;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;

/**
 * This is a JAX-RS based wrapper that performs GraphQL client related tests.
 * We can't perform these tests in the `@Test` methods directly, because the GraphQL client
 * relies on CDI, and CDI is not available in native mode on the `@Test` side.
 * Therefore the test only calls this REST endpoint which then performs all the client related work.
 */
@Path("/")
public class GraphQLClientTester {

    @GET
    @Path("/typesafe/{url}")
    public void typesafe(@PathParam("url") String url) {
        LuckyNumbersClientApi client = TypesafeGraphQLClientBuilder.newBuilder()
                .endpoint(url + "/graphql")
                .build(LuckyNumbersClientApi.class);
        client.setLuckyNumber(21);
        Integer returned = client.luckyNumber();
        if (!returned.equals(21)) {
            throw new RuntimeException("Returned number was: " + returned);
        }
    }

    @GET
    @Path("/typesafe-header/{url}")
    public void typesafeHeaders(@PathParam("url") String url) {
        LuckyNumbersClientApiWithConfigKey client = TypesafeGraphQLClientBuilder.newBuilder()
                .configKey("lucky")
                .endpoint(url + "/graphql")
                .build(LuckyNumbersClientApiWithConfigKey.class);
        String header = client.returnHeader("myheader");
        if (!"myvalue".equals(header)) {
            throw new RuntimeException("Header 'myheader' was: " + header);
        }
    }

    @GET
    @Path("/typesafe-non-null/{url}")
    public void typesafeNonNull(@PathParam("url") String url) {
        LuckyNumbersClientApi client = TypesafeGraphQLClientBuilder.newBuilder()
                .endpoint(url + "/graphql")
                .build(LuckyNumbersClientApi.class);
        List<Integer> someNumbers = List.of(12, 33);
        List<Integer> returned = client.echoList(someNumbers);
        if (!returned.equals(someNumbers)) {
            throw new RuntimeException("Returned numbers were: " + returned);
        }
    }

    @GET
    @Path("/dynamic/{url}")
    public void dynamic(@PathParam("url") String url) throws Exception {
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder().url(url + "/graphql").build()) {
            Document setLuckyNumberMutation = document(
                    operation(OperationType.MUTATION,
                            field("set",
                                    args(arg("newLuckyNumber", 15)))));
            Document getLuckyNumberQuery = document(
                    operation("number", field("get")));

            // set the lucky number to 15
            Response response = client.executeSync(setLuckyNumberMutation);
            int returnedNumber = response.getData().getInt("set");
            if (returnedNumber != 15) {
                throw new RuntimeException("Unexpected response: " + response);
            }

            // get the lucky number and assert that it's 15
            response = client.executeSync(getLuckyNumberQuery);
            returnedNumber = response.getData().getInt("get");
            if (returnedNumber != 15) {
                throw new RuntimeException("Unexpected response: " + response);
            }
        }
    }

    @GET
    @Path("/dynamic-subscription/{url}")
    public void dynamicSubscription(@PathParam("url") String url) throws Exception {
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder().url(url.toString() + "/graphql").build()) {
            Document op = document(
                    operation(OperationType.SUBSCRIPTION,
                            field("primeNumbers")));
            List<Integer> expectedNumbers = List.of(2, 3, 5, 7, 11, 13);
            List<Response> responses = client.subscription(op)
                    .subscribe()
                    .asStream()
                    .collect(Collectors.toList());
            for (int i = 0; i < expectedNumbers.size(); i++) {
                if (expectedNumbers.get(i) != responses.get(i).getData().getInt("primeNumbers")
                        || responses.get(i).hasError()) {
                    throw new RuntimeException("Unexpected response: " + responses.get(i));
                }
            }
        }
    }

}
