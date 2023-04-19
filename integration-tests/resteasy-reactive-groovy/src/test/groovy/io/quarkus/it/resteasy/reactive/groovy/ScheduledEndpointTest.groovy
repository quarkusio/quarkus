package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given

@QuarkusTest
class ScheduledEndpointTest {

    @Test
    void testScheduledMethodWithNoArg() {
        given()
            .when()
            .get("/scheduled/num1")
            .then()
            .statusCode(201)
    }

    @Test
    void testScheduledMethodWithScheduledExecution() {
        given()
            .when()
            .get("/scheduled/num2")
            .then()
            .statusCode(201)
    }
}
