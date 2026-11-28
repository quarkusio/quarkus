package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PrimitiveResponseTest {

    @ParameterizedTest
    @CsvSource({
            "long, 9876543210",
            "int, 42",
            "double, 3.5",
            "boolean, true",
            "Long, 9876543210"
    })
    void shouldReadTextResponseAsPrimitive(String type, String value) {
        given()
                .queryParam("value", value)
                .when().get("/client/primitive/" + type)
                .then()
                .statusCode(200)
                .body(equalTo(value));
    }
}
