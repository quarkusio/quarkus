package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DomResourceTest {

    @Test
    public void testSerialize() {
        given()
                .contentType("application/xml")
                .body("<greeting>hello</greeting>")
                .when().post("/dom/serialize")
                .then()
                .statusCode(200)
                .body(equalTo("\"<greeting>hello</greeting>\""));
    }

    @Test
    public void testDeserialize() {
        given()
                .contentType("application/json")
                .body("\"<greeting>hello</greeting>\"")
                .when().post("/dom/deserialize")
                .then()
                .statusCode(200)
                .body(equalTo("greeting:hello"));
    }
}
