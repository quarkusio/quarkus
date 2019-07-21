package io.quarkus.it.caffeine;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class CaffeineTest {
    @Test
    public void testCAche() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("val-1")
                .put("/test/cache/key-1");

        RestAssured.when()
                .get("/test/cache/key-1").then().body(is("val-1"));
    }
}
