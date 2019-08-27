package org.acme

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`

@QuarkusTest
class HelloResourceTest {

    @Test
    fun testHelloEndpoint() {
        given()
                .`when`().get("/app/hello")
                .then()
                .statusCode(200)
                .body(`is`("hello"))
    }

}
