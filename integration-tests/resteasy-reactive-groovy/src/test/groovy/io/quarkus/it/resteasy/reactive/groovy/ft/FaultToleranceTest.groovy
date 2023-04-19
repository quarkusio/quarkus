package io.quarkus.it.resteasy.reactive.groovy.ft

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.equalTo

@QuarkusTest
class FaultToleranceTest {
    @Test
    void test() {
        given()
            .when()
            .post("/ft/hello/fail")
            .then()
            .statusCode(204)
        given()
            .when()
            .get("/ft/client")
            .then()
            .statusCode(200)
            .body(equalTo("fallback"))
        given()
            .when()
            .post("/ft/hello/heal")
            .then()
            .statusCode(204)
        given()
            .when()
            .get("/ft/client")
            .then()
            .statusCode(200)
            .body(equalTo("Hello, world!"))
    }
}
