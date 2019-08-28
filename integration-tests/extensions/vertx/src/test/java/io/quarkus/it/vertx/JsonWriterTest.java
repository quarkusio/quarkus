package io.quarkus.it.vertx;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Thomas Segismont
 */
@QuarkusTest
public class JsonWriterTest {

    @Test
    public void testJsonSync() {
        RestAssured.when().get("/vertx-test/json-bodies/json/sync").then()
                .statusCode(200).body("Hello", equalTo("World"));
    }

    @Test
    public void testArraySync() {
        RestAssured.when().get("/vertx-test/json-bodies/array/sync").then()
                .statusCode(200).body("", equalTo(Arrays.asList("Hello", "World")));
    }

    @Test
    public void testJsonAsync() {
        RestAssured.when().get("/vertx-test/json-bodies/json/async").then()
                .statusCode(200).body("Hello", equalTo("World"));
    }

    @Test
    public void testArrayAsync() {
        RestAssured.when().get("/vertx-test/json-bodies/array/async").then()
                .statusCode(200).body("", equalTo(Arrays.asList("Hello", "World")));
    }
}
