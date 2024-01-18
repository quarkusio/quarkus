package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

@QuarkusTest
class CompanionTest {

    @Test
    fun testSuccessResponseData() {
        When { get("/companion/success") } Then
            {
                statusCode(200)
                body(containsString("200"))
            }
    }

    @Test
    fun testFailureResponseData() {
        When { get("/companion/failure") } Then
            {
                statusCode(200)
                body(containsString("500"), containsString("error"))
            }
    }
}
