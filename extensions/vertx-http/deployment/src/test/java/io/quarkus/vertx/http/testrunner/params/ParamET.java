package io.quarkus.vertx.http.testrunner.params;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParamET {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 4, 11, 17 })
    void shouldValidateOddNumbers(int x) {
        given()
                .when().get("/odd/" + x)
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
