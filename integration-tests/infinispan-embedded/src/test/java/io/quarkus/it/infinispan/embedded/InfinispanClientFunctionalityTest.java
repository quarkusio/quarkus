package io.quarkus.it.infinispan.embedded;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author William Burns
 */
@QuarkusTest
public class InfinispanClientFunctionalityTest {
    @Test
    public void testCache() {
        // This cache also has persistence
        testCache("local");
    }

    @Test
    public void testOffHeapCache() {
        testCache("off-heap-memory");
    }

    @Test
    public void testTransactionRolledBack() {
        String cacheName = "quarkus-transaction";
        System.out.println("Running cache test for " + cacheName);
        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("null"));
        // This should throw an exception and NOT commit the value
        RestAssured.when().get("/test/PUT/" + cacheName + "/key/something?shouldFail=true")
                .then()
                .statusCode(500);
        // Entry shouldn't have been committed
        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("null"));
    }

    @Test
    public void testPutWithoutTransactionNotRolledBack() {
        String cacheName = "simple-cache";
        System.out.println("Running cache test for " + cacheName);
        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("null"));
        // This should throw an exception - but cache did put before
        RestAssured.when().get("/test/PUT/" + cacheName + "/key/something?shouldFail=true")
                .then()
                .statusCode(500);
        // Entry should be available
        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("something"));
    }

    private void testCache(String cacheName) {
        System.out.println("Running cache test for " + cacheName);
        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("null"));

        RestAssured.when().get("/test/PUT/" + cacheName + "/key/something").then().body(is("null"));

        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("something"));

        RestAssured.when().get("/test/REMOVE/" + cacheName + "/key").then().body(is("something"));

        RestAssured.when().get("/test/GET/" + cacheName + "/key").then().body(is("null"));
    }
}
