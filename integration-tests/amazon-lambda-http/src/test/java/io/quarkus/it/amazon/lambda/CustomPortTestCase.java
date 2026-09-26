package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test that verifies custom test port configuration is respected
 * by @TestHTTPResource and @TestHTTPEndpoint annotations.
 * <p>
 * This test addresses the bug where the Lambda HTTP extension
 * ignored custom port configuration, always defaulting to 8081.
 * </p>
 */
@QuarkusTest
public class CustomPortTestCase {

    @TestHTTPResource
    @TestHTTPEndpoint(GreetingResource.class)
    URL greetingEndpoint;

    @TestHTTPResource("/hello")
    URL helloUrl;

    /**
     * Verifies that @TestHTTPEndpoint resolves to the custom port (8085)
     * configured in application.properties, not the default port (8081).
     */
    @Test
    public void testCustomPortWithEndpointAnnotation() {
        // The URL should use port 8085 as configured in test application.properties
        assertEquals(8085, greetingEndpoint.getPort(),
                "@TestHTTPEndpoint should use custom test port 8085");
        assertTrue(greetingEndpoint.toString().contains(":8085"),
                "URL should contain custom port 8085, but was: " + greetingEndpoint);

        // Verify the endpoint is actually accessible on the custom port
        given()
                .when()
                .get(greetingEndpoint)
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    /**
     * Verifies that @TestHTTPResource resolves to the custom port (8085)
     * configured in application.properties, not the default port (8081).
     */
    @Test
    public void testCustomPortWithResourceAnnotation() {
        // The URL should use port 8085 as configured in test application.properties
        assertEquals(8085, helloUrl.getPort(),
                "@TestHTTPResource should use custom test port 8085");
        assertTrue(helloUrl.toString().contains(":8085"),
                "URL should contain custom port 8085, but was: " + helloUrl);

        // Verify the endpoint is actually accessible on the custom port
        given()
                .when()
                .get(helloUrl)
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    /**
     * Verifies that RestAssured uses the custom port by default.
     * This ensures the entire test infrastructure respects the custom port.
     */
    @Test
    public void testRestAssuredUsesCustomPort() {
        given()
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }
}
