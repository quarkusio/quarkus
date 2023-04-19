package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is

@QuarkusTest
class ReactiveGreetingResourceTest {

    @Test
    void testResource() {
        given()
            .when()
            .get("/test.txt")
            .then()
            .statusCode(200)
    }

    @Test
    void testHello() {
        given()
            .when()
            .get("/hello-resteasy-reactive/")
            .then()
            .statusCode(200)
            .body(is("Hello RestEASY Reactive"))
    }

    @Test
    void testStandard() {
        given()
            .when()
            .get("/hello-resteasy-reactive/standard")
            .then()
            .statusCode(200)
            .body(is("Hello RestEASY Reactive"))
    }

    @Test
    void testNamedHello() {
        given()
            .when()
            .get("/hello-resteasy-reactive/Bob")
            .then()
            .statusCode(200)
            .body(is("Hello Bob"))
    }
}
