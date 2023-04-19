package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.notNullValue

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testDataClassAndCustomFilters() {
        given()
            .when()
            .get("/greeting")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("message", is("hello foo bar"))
            .header("method", "testSuspend")
            .header("method2", "testSuspend")
    }

    @Test
    void testAbortingCustomFilters() {
        given()
            .header("abort", "true")
            .when()
            .get("/greeting")
            .then()
            .statusCode(204)
            .header("random", notNullValue())
    }

    @Test
    void testNoopCoroutine() {
        given()
            .when()
            .get("/greeting/noop")
            .then()
            .statusCode(204)
    }
}
