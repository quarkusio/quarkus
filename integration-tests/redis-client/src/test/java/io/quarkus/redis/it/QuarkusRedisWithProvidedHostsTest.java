package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisWithProvidedHostsTest {
    static final String SYNC_KEY = "named-sync-key";
    static final String SYNC_VALUE = "named-sync-value";

    static final String REACTIVE_KEY = "named-reactive-key";
    static final String REACTIVE_VALUE = "named-reactive-value";

    @Test
    public void sync() {
        RestAssured.given()
                .when()
                .get("/quarkus-redis-provided-hosts/sync/" + SYNC_KEY)
                .then()
                .statusCode(204); // the key is not set yet

        RestAssured.given()
                .body(SYNC_VALUE)
                .when()
                .post("/quarkus-redis-provided-hosts/sync/" + SYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis-provided-hosts/sync/" + SYNC_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(SYNC_VALUE));
    }

    @Test
    public void reactive() {
        RestAssured.given()
                .when()
                .get("/quarkus-redis-provided-hosts/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(204); // the reactive key is not set yet

        RestAssured.given()
                .body(REACTIVE_VALUE)
                .when()
                .post("/quarkus-redis-provided-hosts/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis-provided-hosts/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(REACTIVE_VALUE));
    }
}
