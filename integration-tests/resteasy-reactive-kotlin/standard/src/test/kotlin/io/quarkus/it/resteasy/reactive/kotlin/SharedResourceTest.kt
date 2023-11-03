package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class SharedResourceTest {

    @Test
    fun testReturnAsIs() {
        Given {
            body("""{ "message": "will not be used" }""")
            contentType(ContentType.JSON)
        } When
            {
                post("/shared")
            } Then
            {
                statusCode(200)
                body(CoreMatchers.`is`("""{"message": "canned+canned"}"""))
            }
    }

    @Test
    fun testApplicationSuppliedProviderIsPreferred() {
        Given {
            body("""{ "message": "will not be used" }""")
            contentType(ContentType.TEXT)
            accept(ContentType.TEXT)
        } When
            {
                post("/shared")
            } Then
            {
                statusCode(200)
                body(CoreMatchers.`is`("""{"message": "app+app"}"""))
            }
    }
}
