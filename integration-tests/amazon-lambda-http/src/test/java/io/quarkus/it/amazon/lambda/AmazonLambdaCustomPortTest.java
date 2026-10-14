package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * {@code @TestHTTPResource} and {@code @TestHTTPEndpoint} must honor a custom test port, instead of falling back to
 * 8081, when requests are served by the Lambda mock event server.
 */
@QuarkusTest
@TestProfile(AmazonLambdaCustomPortTest.CustomPortProfile.class)
class AmazonLambdaCustomPortTest {
    @TestHTTPResource
    @TestHTTPEndpoint(GreetingResource.class)
    URL endpoint;

    @Test
    void injectedUrlUsesConfiguredPort() {
        assertEquals("http://localhost:8085/hello", endpoint.toString());
    }

    @Test
    void requestToInjectedUrl() {
        given()
                .header("Access-Control-Request-Headers", "foo, bar, yello")
                .when().get(endpoint + "/comma")
                .then()
                .statusCode(200);
    }

    @Test
    void restAssuredUsesConfiguredPort() {
        given()
                .header("Access-Control-Request-Headers", "foo, bar, yello")
                .when().get("/hello/comma")
                .then()
                .statusCode(200);
    }

    public static class CustomPortProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.test-port", "8085",
                    "quarkus.lambda.mock-event-server.test-port", "8085");
        }
    }
}
