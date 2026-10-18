package io.quarkus.it.openapi.security;

import static io.quarkus.it.openapi.security.TestSecurityResource.TEST_HEADER_NAME;
import static io.quarkus.it.openapi.security.TestSecurityResource.TEST_HEADER_VALUE;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.hamcrest.Matchers.is;

import java.net.SocketTimeoutException;
import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;

@QuarkusTest
@TestHTTPEndpoint(TestSecurityResource.class)
public class TestSecurityReactiveRoutesTest {

    @TestSecurity(user = "Martin", roles = "admin")
    @Test
    public void testSecurityWithReactiveRoutesAndQuarkusRest() {
        RestAssured.get("reactive-routes")
                .then()
                .statusCode(200)
                .header("reactive-routes-filter", is("true"))
                .body(is("Martin"));
    }

    /**
     * This verifies that CDI request context activated by Reactive Routes is not deactivated/destroyed while
     * Quarkus REST needs it. Before the fix, there was a racy behavior. Sometimes during the CDI interceptors processing,
     * sometimes after the socket timeout when resource method was executed, CDI request context was not active.
     * Depending on the speed of a test executor, you may need to execute this test couple of times in order to reproduce
     * the original issue.
     */
    @TestSecurity(user = "Martin", roles = "admin")
    @Test
    public void testCdiRequestActiveAfterTimeout() throws InterruptedException {
        RestAssured.delete("empty-failure-storage").then().statusCode(204);

        int valueLesserThanDelay = TestSecurityResource.REQUEST_TIMEOUT - 2;
        // using deprecated constant due to https://github.com/rest-assured/rest-assured/issues/497
        var config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig().setParam(SO_TIMEOUT, valueLesserThanDelay));
        try {
            RestAssured.given()
                    .config(config)
                    .header(TEST_HEADER_NAME, TEST_HEADER_VALUE)
                    .get("reactive-routes-with-delayed-response")
                    .then()
                    .statusCode(200)
                    .header("reactive-routes-filter", is("true"))
                    .body(is("Martin"));
            Assertions.fail("HTTP request didn't result in a socket timeout exception");
        } catch (Exception socketTimeoutException) {
            // yes, this checked exception is thrown even though no method signature declares it
            if (!(socketTimeoutException instanceof SocketTimeoutException)) {
                // socket timeout exception is required to verify what happens with the CDI request context after the timeout
                Assertions.fail("Expected a SocketTimeoutException but got " + socketTimeoutException);
            }
        }
        int timeoutRemainder = TestSecurityResource.REQUEST_TIMEOUT - valueLesserThanDelay + 1;
        Thread.sleep(Duration.ofSeconds(timeoutRemainder).toMillis());
        RestAssured.given()
                .get("throwable")
                .then()
                .statusCode(200)
                .header("reactive-routes-filter", is("true"))
                .body(is("null"));
    }

}
