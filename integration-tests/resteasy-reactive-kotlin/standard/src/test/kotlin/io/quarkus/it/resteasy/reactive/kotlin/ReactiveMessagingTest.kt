package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.get
import io.restassured.common.mapper.TypeRef
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
@QuarkusTestResource(KafkaTestResource::class)
class ReactiveMessagingTest {

    private val TYPE_REF: TypeRef<List<Country>> = object: TypeRef<List<Country>>() {}

    @Test
    fun test() {
        await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(5)).untilAsserted {
            assertEquals(get("/country/resolved").`as`(TYPE_REF).size, 6)
        }
    }
}