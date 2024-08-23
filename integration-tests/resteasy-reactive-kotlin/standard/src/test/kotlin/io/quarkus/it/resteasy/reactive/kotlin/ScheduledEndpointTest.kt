package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.Test

@QuarkusTest
class ScheduledEndpointTest {

    @Test
    fun testScheduledMethodWithNoArg() {
        When { get("/scheduled/num1") } Then { statusCode(201) }
    }

    @Test
    fun testScheduledMethodWithScheduledExecution() {
        When { get("/scheduled/num2") } Then { statusCode(201) }
    }
}
