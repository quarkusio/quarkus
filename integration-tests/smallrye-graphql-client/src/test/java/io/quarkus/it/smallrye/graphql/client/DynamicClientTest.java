package io.quarkus.it.smallrye.graphql.client;

import static io.restassured.RestAssured.when;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * See `GraphQLClientTester` for the actual testing code that uses GraphQL clients.
 */
@QuarkusTest
public class DynamicClientTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testDynamicClient() {
        when()
                .get("/dynamic/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    @Test
    public void testDynamicClientSubscription() throws Exception {
        when()
                .get("/dynamic-subscription/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

}
