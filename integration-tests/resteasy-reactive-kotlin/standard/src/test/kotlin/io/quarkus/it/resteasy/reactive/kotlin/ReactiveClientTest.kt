package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class ReactiveClientTest {

    @Test
    fun testGetCountryByName() {
        RestAssured.given()
                .`when`()["/country/name/foo"]
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size()", CoreMatchers.`is`(1),
                        "[0].capital", CoreMatchers.`is`("foo-capital"))
    }
}
