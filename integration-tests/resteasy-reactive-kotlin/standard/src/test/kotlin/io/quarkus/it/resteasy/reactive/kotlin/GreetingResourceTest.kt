package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetingResourceTest {

    @Test
    fun testDataClassAndCustomFilters() {
        When {
            get("/greeting")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("message", CoreMatchers.`is`("hello foo bar"))
            header("method", "testSuspend")
        }
    }

    @Test
    fun testAbortingCustomFilters() {
        Given {
            header("abort", "true")
        } When {
            get("/greeting")
        } Then {
            statusCode(204)
        }
    }

    @Test
    fun testNoopCoroutine() {
        When {
            get("/greeting/noop")
        } Then {
            statusCode(204)
        }
    }
}
