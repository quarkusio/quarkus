package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisTest {
    private static final String SYNC_KEY = "sync-key";
    private static final String SYNC_VALUE = "sync-value";
    private static final String ANOTHER_SYNC_VALUE = "sync-value";

    private static final String ASYNC_KEY = "async-key";
    private static final String ASYNC_VALUE = "async-value";

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

        RestAssured.given()
                .body(ANOTHER_SYNC_VALUE)
                .when()
                .post("/quarkus-redis/sync-bytes/" + SYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/sync/" + SYNC_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(ANOTHER_SYNC_VALUE));
    }

    @Test
    public void async() {
        RestAssured.given()
                .body(ASYNC_VALUE)
                .when()
                .post("/quarkus-redis/async/" + ASYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/async/" + ASYNC_KEY)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(ASYNC_VALUE));
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

    @Test
    public void hashes() {
        RestAssured.given()
                .body(REACTIVE_VALUE)
                .when()
                .post("/quarkus-redis/hashes/" + REACTIVE_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .body(SYNC_VALUE)
                .when()
                .post("/quarkus-redis/hashes/" + SYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .body(ASYNC_VALUE)
                .when()
                .post("/quarkus-redis/hashes/" + ASYNC_KEY)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/hashes/")
                .then()
                .statusCode(200)
                .body(SYNC_KEY, CoreMatchers.is(SYNC_VALUE))
                .body(ASYNC_KEY, CoreMatchers.is(ASYNC_VALUE))
                .body(REACTIVE_KEY, CoreMatchers.is(REACTIVE_VALUE));
    }

    @Test
    public void lists() {
        RestAssured.given()
                .body(REACTIVE_VALUE)
                .when()
                .post("/quarkus-redis/lists/")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body(ASYNC_VALUE)
                .when()
                .post("/quarkus-redis/lists/")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body(SYNC_VALUE)
                .when()
                .post("/quarkus-redis/lists/")
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get("/quarkus-redis/lists/")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(SYNC_VALUE));

        RestAssured.given()
                .when()
                .get("/quarkus-redis/lists/")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(ASYNC_VALUE));

        RestAssured.given()
                .when()
                .get("/quarkus-redis/lists/")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(REACTIVE_VALUE));
    }

    @Test
    public void geos() {
        RestAssured.given()
                .when()
                .get("/quarkus-redis/geos/")
                .then()
                .statusCode(200);
    }

    @Test
    public void testDefaultMethodCall() {
        RestAssured.given()
                .when()
                .get("/quarkus-redis/default/" + SYNC_KEY)
                .then()
                .statusCode(200);
    }
}
