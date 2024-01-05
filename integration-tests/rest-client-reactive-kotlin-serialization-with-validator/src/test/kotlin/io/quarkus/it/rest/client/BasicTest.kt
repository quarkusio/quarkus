package io.quarkus.it.rest.client

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@QuarkusTest
open class BasicTest {

    @Test
    fun valid() {
        val response = RestAssured.with().get("/validate/{id}", "12345")
        Assertions.assertThat(response.asString()).isEqualTo("12345")
    }

    @Test
    fun invalid() {
        val response = RestAssured.with().get("/validate/{id}", "1234")
        Assertions.assertThat(response.asString())
            .contains("Constraint Violation")
            .contains("validate.id")
            .contains("string is too short")
    }
}
