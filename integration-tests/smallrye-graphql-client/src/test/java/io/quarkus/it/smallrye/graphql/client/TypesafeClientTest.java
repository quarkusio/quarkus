package io.quarkus.it.smallrye.graphql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.io.smallrye.graphql.client.LuckyNumbersClientApi;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;

@QuarkusTest
public class TypesafeClientTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testTypesafeClient() throws Exception {
        LuckyNumbersClientApi client = TypesafeGraphQLClientBuilder.newBuilder()
                .endpoint(url.toString() + "/graphql")
                .build(LuckyNumbersClientApi.class);
        client.setLuckyNumber(21);
        assertEquals(21, client.luckyNumber());
    }

    /**
     * Test a method that contains a `@NonNull` parameter.
     */
    @Test
    public void testNonNull() throws Exception {
        LuckyNumbersClientApi client = TypesafeGraphQLClientBuilder.newBuilder()
                .endpoint(url.toString() + "/graphql")
                .build(LuckyNumbersClientApi.class);
        List<Integer> someNumbers = List.of(12, 33);
        List<Integer> returned = client.echoList(someNumbers);
        assertEquals(someNumbers, returned);
    }
}
