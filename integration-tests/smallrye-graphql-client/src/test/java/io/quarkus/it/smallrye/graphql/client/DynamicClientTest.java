package io.quarkus.it.smallrye.graphql.client;

import static io.restassured.RestAssured.when;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;

/**
 * See `GraphQLClientTester` for the actual testing code that uses GraphQL clients.
 */
@QuarkusTest
public class DynamicClientTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testDynamicClientSingleResultOperationOverHttp() {
        when()
                .get("/dynamic-single-http/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    @Test
    public void testDynamicClientSingleResultOperationOverWebSocket() {
        when()
                .get("/dynamic-single-websocket/" + url.toString())
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

    @Test
    @DisabledOnIntegrationTest(forArtifactTypes = DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY)
    public void testDynamicClientAutowiredUrl() throws Exception {
        when()
                .get("/autowired-dynamic/")
                .then()
                .log().everything()
                .statusCode(204);
    }

    @Test
    public void testDynamicClientDirective() throws Exception {
        when()
                .get("/dynamic-directive/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

}
