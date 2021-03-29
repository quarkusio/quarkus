package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisTest {
    static final String SYNC_KEY = "sync-key";
    static final String SYNC_VALUE = "sync-value";
    static final String REACTIVE_KEY = "reactive-key";
    static final String REACTIVE_VALUE = "reactive-value";

    @Test
    public void sync() {
        RestAssured.given()
                .when()
                .get("/quarkus-redis/sync/" + SYNC_KEY)
                .then()
                .statusCode(204); // the key is not set yet

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
                .when()
                .get("/quarkus-redis/reactive/" + REACTIVE_KEY)
                .then()
                .statusCode(204); // the reactive key is not set yet

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
