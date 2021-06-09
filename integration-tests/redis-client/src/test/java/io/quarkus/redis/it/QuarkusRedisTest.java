package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisTest {
    static final String SYNC_KEY = "named-sync-key";
    static final String SYNC_VALUE = "named-sync-value";

    static final String REACTIVE_KEY = "named-reactive-key";
    static final String REACTIVE_VALUE = "named-reactive-value";

    static final String[] BASE_URLS = { "/quarkus-redis",
            "/quarkus-redis-with-named",
            "/quarkus-redis-dynamic-client-creation",
            "/quarkus-redis-provided-hosts",
            "/quarkus-redis-parameter-injection" };

    @Test
    public void sync() {
        for (String baseUrl : BASE_URLS) {
            String path = String.format("%s/sync/%s", baseUrl, SYNC_KEY);
            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(204); // the key is not set yet

            RestAssured.given()
                    .body(SYNC_VALUE)
                    .when()
                    .post(path)
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body(CoreMatchers.is(SYNC_VALUE));
        }
    }

    @Test
    public void reactive() {
        for (String baseUrl : BASE_URLS) {
            String path = String.format("%s/reactive/%s", baseUrl, REACTIVE_KEY);
            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(204); // the reactive key is not set yet

            RestAssured.given()
                    .body(REACTIVE_VALUE)
                    .when()
                    .post(path)
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body(CoreMatchers.is(REACTIVE_VALUE));
        }
    }
}
