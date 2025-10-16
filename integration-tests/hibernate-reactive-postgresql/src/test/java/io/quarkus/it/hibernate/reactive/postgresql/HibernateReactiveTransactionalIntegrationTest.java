package io.quarkus.it.hibernate.reactive.postgresql;

import static io.restassured.config.HttpClientConfig.*;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.quarkus.logging.Log;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Integration tests for @Transactional support with Hibernate Reactive.
 *
 * These tests verify that transactional boundaries are properly managed when using
 * the @Transactional annotation with reactive Hibernate operations.
 */
@QuarkusTest
@TestHTTPEndpoint(HibernateReactiveTransactionalTestEndpoint.class)
public class HibernateReactiveTransactionalIntegrationTest {

    @Test
    public void testTransactionalUpdate() {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .post("/createPig/20")
                .then()
                .body("id", equalTo(20))
                .body("name", equalTo("initialName"));

        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .post("/updatePig/20?name=updatedName")
                .then()
                .statusCode(204);

        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/findPig/20")
                .then()
                .body("id", equalTo(20))
                .body("name", equalTo("updatedName"));
    }

    /**
     * Tests that transaction rollback occurs when the HTTP client cancels a request that is
     * in the middle of a transactional operation.
     *
     * <p>
     * The endpoint returns Uni.createFrom().nothing() which never completes, causing the client
     * to timeout and cancel the request. This simulates a real-world scenario where a client
     * cancels a long-running request. The test verifies that the transaction is properly rolled
     * back and no changes are persisted.
     *
     * <p>
     * <b>Current Limitations:</b>
     * <ul>
     * <li>The timeout must be long enough for the Uni chain to reach the hanging point after
     * flush(). If the timeout is too short, the cancellation may occur before the transaction
     * begins, resulting in different behavior (no commit but also no explicit rollback).</li>
     * <li>Transaction cancellation can only be verified by examining the logs for cancellation
     * messages. There is currently no programmatic way to assert that cancellation occurred.</li>
     * <li>This test uses a client-side timeout to trigger cancellation. A more direct approach
     * using server-side cancellation mechanisms would be preferable but was not achievable
     * with the current implementation.</li>
     * </ul>
     */
    @Test
    public void testTransactionalCancellation() throws InterruptedException {
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .post("/createPig/30")
                .then()
                .body("id", equalTo(30))
                .body("name", equalTo("initialName"));

        // Start the hanging update asynchronously
        CompletableFuture<Void> hangingUpdate = CompletableFuture.runAsync(() -> {
            try {
                RestAssured.given()
                        .config(RestAssured.config()
                                .httpClient(httpClientConfig()
                                        // 400ms timeout - long enough for the operation to flush and hang
                                        .setParam("http.socket.timeout", 400)))
                        .when()
                        .auth().preemptive().basic("scott", "jb0ss")
                        .post("/updatePigHanging/30?name=updatedNameHanging")
                        .then()
                        .statusCode(204);
            } catch (Exception e) {
                Log.debugf("(expected) failure due to timeout after 400ms");
            }
        });

        // Wait for the hanging update to complete
        hangingUpdate.join();

        // Verify the name was NOT updated (rollback happened)
        RestAssured.given().when()
                .auth().preemptive().basic("scott", "jb0ss")
                .get("/findPig/30")
                .then()
                .body("id", equalTo(30))
                .body("name", equalTo("initialName"));
    }
}
