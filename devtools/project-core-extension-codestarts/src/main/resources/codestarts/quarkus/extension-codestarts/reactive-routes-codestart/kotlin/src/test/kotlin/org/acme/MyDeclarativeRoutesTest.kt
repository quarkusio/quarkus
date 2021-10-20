package org.acme

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class ReactiveGreetingResourceTest {

    @Test
    fun testHelloEndpoint() {
        given()
          .`when`().get("/hello")
          .then()
             .statusCode(200)
             .body(`is`("Hello RESTEasy Reactive Route"))
    }

    @Test
    fun testWorldEndpoint() {
        given()
                .`when`().get("/world")
                .then()
                .statusCode(200)
                .body(`is`("Hello world !!"))
    }

    @Test
    fun testGreetingEndpointWithNameParameter() {
        given()
                .`when`().get("/greetings?name=Quarkus")
                .then()
                .statusCode(200)
                .body(`is`("Hello  Quarkus !!"))
    }

    @Test
    fun testGreetingEndpointWithoutNameParameter() {
        given()
                .`when`().get("/greetings")
                .then()
                .statusCode(200)
                .body(`is`("Hello  RESTEasy Reactive Route !!"))
    }
}