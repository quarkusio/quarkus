package io.quarkus.it.rest.client

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@QuarkusTest
open class BasicTest {

    @TestHTTPResource("/") lateinit var country: String

    @Test
    fun callCountry() {
        val response = RestAssured.with().body(country).post("/call-country")
        Assertions.assertThat(response.asString()).isEqualTo("Sthlm")
    }

    @Test
    fun callCountries() {
        val response = RestAssured.with().body(country).post("/call-countries")
        Assertions.assertThat(response.asString()).isEqualTo("OK")
    }

    @Test
    fun callNotFoundCountries() {
        val response = RestAssured.with().body(country).post("/call-not-found-countries")
        Assertions.assertThat(response.statusCode).isEqualTo(204)
    }
}
