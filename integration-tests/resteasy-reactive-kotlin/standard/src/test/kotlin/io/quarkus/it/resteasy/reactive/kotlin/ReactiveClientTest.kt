package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class ReactiveClientTest {

    @Test
    fun testGetCountryByName() {
        When { get("/country/name/foo") } Then
            {
                statusCode(200)
                contentType(ContentType.JSON)
                body(
                    "$.size()",
                    CoreMatchers.`is`(1),
                    "[0].capital",
                    CoreMatchers.`is`("foo-capital"),
                )
            }
    }
}
