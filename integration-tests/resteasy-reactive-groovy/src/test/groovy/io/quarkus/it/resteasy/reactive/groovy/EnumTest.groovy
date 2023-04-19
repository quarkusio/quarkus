package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.equalTo

@QuarkusTest
class EnumTest {

    @Test
    void testNoStates() {
        given()
            .when()
            .get("/enum")
            .then()
            .statusCode(200)
            .body(equalTo("States: []"))
    }

    @Test
    void testSingleState() {
        given()
                .when()
                .get("/enum?state=State1")
                .then()
                .statusCode(200)
                .body(equalTo("States: [State1]"))
    }

    @Test
    void testMultipleStates() {
        given()
                .when()
                .get("/enum?state=State1&state=State2&state=State3")
                .then()
                .statusCode(200)
                .body(equalTo("States: [State1, State2, State3]"))
    }
}
