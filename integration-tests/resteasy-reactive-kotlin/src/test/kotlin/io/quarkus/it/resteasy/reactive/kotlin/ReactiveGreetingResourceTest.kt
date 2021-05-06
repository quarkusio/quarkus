package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class ReactiveGreetingResourceTest {

    @Test
    fun testResource() {
        given()
                .`when`().get("/test.txt")
                .then()
                .statusCode(200)

        given()
                .`when`().get("/test2.txt")
                .then()
                .statusCode(404)
    }

    @Test
    fun testHello() {
        given()
            .`when`().get("/hello-resteasy-reactive/")
            .then()
            .statusCode(200)
            .body(`is`("Hello RestEASY Reactive"))
    }

    @Test
    fun testStandard() {
        given()
            .`when`().get("/hello-resteasy-reactive/standard")
            .then()
            .statusCode(200)
            .body(`is`("Hello RestEASY Reactive"))
    }

    @Test
    fun testNamedHello() {
        given()
            .`when`().get("/hello-resteasy-reactive/Bob")
            .then()
            .statusCode(200)
            .body(`is`("Hello Bob"))
    }
}
