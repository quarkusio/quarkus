package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MethodHandleResourceTest {

    @Test
    public void testReflectionBasedSerializationAndDeserialization() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "creatorValue": "creator",
                          "fieldValue": "field",
                          "setterValue": "setter"
                        }
                        """)
                .when().post("/method-handle")
                .then()
                .statusCode(200)
                .body("creatorValue", equalTo("creator"))
                .body("fieldValue", equalTo("field"))
                .body("setterValue", equalTo("setter"));
    }
}
