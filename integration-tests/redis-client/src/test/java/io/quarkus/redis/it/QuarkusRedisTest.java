package io.quarkus.redis.it;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class QuarkusRedisTest {
    static final String SYNC_KEY = "named-sync-key";
    static final String SYNC_VALUE = "named-sync-value";

    static final String REACTIVE_KEY = "named-reactive-key";
    static final String REACTIVE_VALUE = "named-reactive-value";

    static final String[] BASE_URLS = { "" +
            "/quarkus-redis",
            "/quarkus-redis-with-name",
            "/quarkus-redis-with-name-legacy",
            "/quarkus-redis-with-instance",
            "/quarkus-redis-provided-hosts",
            "/quarkus-redis-parameter-injection-legacy"
    };

    String getKey(String k) {
        return k;
    }

    @Test
    public void sync() {
        for (String baseUrl : BASE_URLS) {
            String path = String.format("%s/sync/%s", baseUrl, getKey(SYNC_KEY));
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
            String path = String.format("%s/reactive/%s", baseUrl, getKey(REACTIVE_KEY));
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

    @Test
    public void testPreloading() {
        RestAssured.get("/quarkus-redis/import").then()
                .statusCode(200)
                .body(Matchers.equalTo("6"));
    }

    @Test
    public void testCustomCodec() {
        String path = "/quarkus-redis/custom-codec/foo";
        RestAssured.given()
                .when()
                .get(path)
                .then()
                .statusCode(204); // the key is not set yet

        RestAssured.given()
                .header("Content-Type", "application/json")
                .body(new Person("bob", "morane"))
                .when()
                .post(path)
                .then()
                .statusCode(204);

        var person = RestAssured.given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .extract().as(Person.class);
        Assertions.assertEquals(person.firstName, "bob");
        Assertions.assertEquals(person.lastName, "MORANE");
    }
}
