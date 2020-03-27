package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class WithoutMocksTest {

    @Test
    public void testGreet() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("HELLO"));
    }

    @Test
    public void testDummy() {
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("first/second"));
    }
}
