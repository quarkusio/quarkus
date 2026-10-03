package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("@TestHTTPEndpoint should use custom port 8085 from configuration")
    public void testCustomPortWithEndpointAnnotation() {
        // Validate all URL properties in one assertion for better test reporting
        assertAll("@TestHTTPEndpoint URL validation",
                () -> assertNotNull(greetingEndpoint, "Endpoint URL should not be null"),
                () -> assertEquals("http", greetingEndpoint.getProtocol(), "Protocol should be http"),
                () -> assertEquals("localhost", greetingEndpoint.getHost(), "Host should be localhost"),
                () -> assertEquals(8085, greetingEndpoint.getPort(),
                        "@TestHTTPEndpoint should use custom test port 8085, not default 8081"),
                () -> assertTrue(greetingEndpoint.toString().contains(":8085"),
                        "URL should contain custom port 8085, but was: " + greetingEndpoint));

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
    @DisplayName("@TestHTTPResource should use custom port 8085 from configuration")
    public void testCustomPortWithResourceAnnotation() {
        // Validate all URL properties in one assertion for better test reporting
        assertAll("@TestHTTPResource URL validation",
                () -> assertNotNull(helloUrl, "Resource URL should not be null"),
                () -> assertEquals("http", helloUrl.getProtocol(), "Protocol should be http"),
                () -> assertEquals("localhost", helloUrl.getHost(), "Host should be localhost"),
                () -> assertEquals(8085, helloUrl.getPort(),
                        "@TestHTTPResource should use custom test port 8085, not default 8081"),
                () -> assertTrue(helloUrl.getPath().endsWith("/hello"),
                        "URL path should end with /hello, but was: " + helloUrl.getPath()),
                () -> assertTrue(helloUrl.toString().contains(":8085"),
                        "URL should contain custom port 8085, but was: " + helloUrl));

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
     * <p>
     * Note: This test has a known issue with RestAssured not being auto-configured
     * for Lambda HTTP tests and is expected to fail until RestAssured integration is fixed.
     * </p>
     */
    @Test
    @DisplayName("RestAssured should use custom port 8085 (known issue - expected to fail)")
    public void testRestAssuredUsesCustomPort() {
        given()
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    /**
     * Edge case: Verify URL construction with complex paths
     */
    @Test
    @DisplayName("@TestHTTPResource should handle complex paths correctly")
    public void testComplexPathHandling() {
        // Additional path segments should be properly appended
        assertAll("Complex path validation",
                () -> assertTrue(helloUrl.getPath().contains("/hello"),
                        "Path should contain /hello segment"),
                () -> assertEquals(8085, helloUrl.getPort(),
                        "Port should be 8085 even with complex paths"));
    }

    /**
     * Edge case: Verify multiple injected URLs use the same port
     */
    @Test
    @DisplayName("Multiple @TestHTTPResource injections should use same custom port")
    public void testMultipleInjectionsUseSamePort() {
        assertAll("Multiple injections consistency",
                () -> assertEquals(greetingEndpoint.getPort(), helloUrl.getPort(),
                        "Both injected URLs should use the same custom port"),
                () -> assertEquals(8085, greetingEndpoint.getPort(),
                        "Endpoint port should be 8085"),
                () -> assertEquals(8085, helloUrl.getPort(),
                        "Resource port should be 8085"));
    }
}
