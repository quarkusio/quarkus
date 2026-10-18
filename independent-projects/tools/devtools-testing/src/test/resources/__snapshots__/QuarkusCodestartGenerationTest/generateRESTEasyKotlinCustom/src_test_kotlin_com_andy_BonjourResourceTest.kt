package com.andy

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class BonjourResourceTest {

    @Test
    fun testHelloEndpoint() {
        given()
          .`when`().get("/bonjour")
          .then()
             .statusCode(200)
             .body(`is`("Hello RESTEasy"))
    }

}