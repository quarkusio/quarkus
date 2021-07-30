package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetingResourceTest {

    @Test
    fun testDataClass() {
        RestAssured.given()
                .`when`()["/greeting"]
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", CoreMatchers.`is`("hello"))
    }
}
