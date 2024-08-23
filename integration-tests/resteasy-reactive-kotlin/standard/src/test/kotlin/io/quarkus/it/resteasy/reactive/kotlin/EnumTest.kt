package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class EnumTest {

    @Test
    fun testNoStates() {
        When { get("/enum") } Then
            {
                statusCode(200)
                body(equalTo("States: []"))
            }
    }

    @Test
    fun testSingleState() {
        When { get("/enum?state=State1") } Then
            {
                statusCode(200)
                body(equalTo("States: [State1]"))
            }
    }

    @Test
    fun testMultipleStates() {
        When { get("/enum?state=State1&state=State2&state=State3") } Then
            {
                statusCode(200)
                body(equalTo("States: [State1, State2, State3]"))
            }
    }
}
