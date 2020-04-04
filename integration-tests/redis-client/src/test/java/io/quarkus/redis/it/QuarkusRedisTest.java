package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisTest {
    private static final String SYNC_KEY = "sync-key";
    private static final String SYNC_VALUE = "sync-value";

    private static final String REACTIVE_KEY = "reactive-key";
    private static final String REACTIVE_VALUE = "reactive-value";

    @Test
    public void sync() {
        RestAssured.given()
                .body(SYNC_VALUE)
                .when()
                .post("/quarkus-redis/sync/" + SYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/sync/" + SYNC_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(SYNC_VALUE));
    }

    @Test
    public void reactive() {
        RestAssured.given()
                .body(REACTIVE_VALUE)
                .when()
                .post("/quarkus-redis/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(REACTIVE_VALUE));
    }
}
