package io.quarkus.it.smallrye.graphql.client;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Argument.args;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.core.OperationType;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@QuarkusTest
public class DynamicClientTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testDynamicClient() throws Exception {
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder().url(url.toString() + "/graphql").build()) {
            Document setLuckyNumberMutation = document(
                    operation(OperationType.MUTATION,
                            field("set",
                                    args(arg("newLuckyNumber", 15)))));
            Document getLuckyNumberQuery = document(
                    operation("number", field("get")));

            // set the lucky number to 15
            Response response = client.executeSync(setLuckyNumberMutation);
            assertEquals(15, response.getData().getInt("set"), response.toString());

            // get the lucky number and assert that it's 15
            response = client.executeSync(getLuckyNumberQuery);
            assertEquals(15, response.getData().getInt("get"), response.toString());
        }
    }
}
