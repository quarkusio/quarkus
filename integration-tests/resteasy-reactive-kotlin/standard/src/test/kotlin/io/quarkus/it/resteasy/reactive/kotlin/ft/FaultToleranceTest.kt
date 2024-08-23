package io.quarkus.it.resteasy.reactive.kotlin.ft

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class FaultToleranceTest {
    @Test
    fun test() {
        When { post("/ft/hello/fail") } Then { statusCode(204) }

        When { get("/ft/client") } Then
            {
                statusCode(200)
                body(equalTo("fallback"))
            }

        When { post("/ft/hello/heal") } Then { statusCode(204) }

        When { get("/ft/client") } Then
            {
                statusCode(200)
                body(equalTo("Hello, world!"))
            }
    }
}
