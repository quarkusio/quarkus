package org.acme.qute

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
class HelloResourceTest {
    @Test
    fun testEndpoint() {
        given()
                .`when`().get("/qute/hello")
                .then()
                .statusCode(200)
                .body(containsString("<p>Hello world!</p>"))
        given()
                .`when`().get("/qute/hello?name=Lucie")
                .then()
                .statusCode(200)
                .body(containsString("<p>Hello Lucie!</p>"))
    }
}