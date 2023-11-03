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
public class TypesafeClientTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testTypesafeClientSingleResultOperationOverHttp() {
        when()
                .get("/typesafe-single-http/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    @Test
    public void testTypesafeClientSingleResultOperationOverWebSocket() {
        when()
                .get("/typesafe-single-websocket/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    /**
     * Test a method that contains a `@NonNull` parameter.
     */
    @Test
    public void testNonNull() {
        when()
                .get("/typesafe-non-null/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    /**
     * Test HTTP headers defined in application.properties.
     */
    @Test
    public void testHeader() {
        when()
                .get("/typesafe-header/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

    @DisabledOnIntegrationTest(forArtifactTypes = DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY)
    @Test
    public void testAutowiredUrl() throws Exception {
        when()
                .get("/autowired-typesafe/")
                .then()
                .log().everything()
                .statusCode(204);
    }

}
