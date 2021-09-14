package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class EnumTest {

    @Test
    fun testNoStates() {
        given()
                .`when`()
                .get("/enum")
                .then()
                .statusCode(200)
                .body(equalTo("States: []"))
    }

    @Test
    fun testSingleState() {
        given()
                .`when`()
                .get("/enum?state=State1")
                .then()
                .statusCode(200)
                .body(equalTo("States: [State1]"))
    }

    @Test
    fun testMultipleStates() {
        given()
                .`when`()
                .get("/enum?state=State1&state=State2&state=State3")
                .then()
                .statusCode(200)
                .body(equalTo("States: [State1, State2, State3]"))
    }
}
