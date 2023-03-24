package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.get
import io.restassured.common.mapper.TypeRef
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import java.time.Duration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(KafkaTestResource::class)
class ReactiveMessagingTest {

    private val TYPE_REF: TypeRef<List<Country>> = object : TypeRef<List<Country>>() {}

    @Test
    fun test() {
        assertCountries(6)

        When { post("/country/kafka/dummy") } Then { statusCode(200) }

        assertCountries(8)
    }

    private fun assertCountries(num: Int) {
        await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(5)).untilAsserted {
            assertEquals(get("/country/resolved").`as`(TYPE_REF).size, num)
        }
    }
}
