package org.acme.qute

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
class ItemsResourceTest {
    @Test
    fun testEndpoint() {
        given()
                .`when`().get("/qute/items")
                .then()
                .statusCode(200)
                .body(containsString("Apple:"), containsString("<del>30</del> <strong>27.0</strong>"))
    }
}