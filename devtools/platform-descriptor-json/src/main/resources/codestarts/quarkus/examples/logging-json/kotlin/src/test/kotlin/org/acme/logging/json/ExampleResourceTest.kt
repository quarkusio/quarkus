package org.acme.logging.json

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

@QuarkusTest
class ExampleResourceTest {
    @Test
    fun testHelloEndpoint() {
        RestAssured.given()
                .`when`().get("/logging-json/")
                .then()
                .statusCode(200)
                .body(CoreMatchers.`is`("hello"))
    }

    @Test
    fun testGoodbyeEndpoint() {
        RestAssured.given()
                .`when`().get("/logging-json/goodbye")
                .then()
                .statusCode(500)
                .body(CoreMatchers.`is`(Matchers.emptyString()))
    }
}