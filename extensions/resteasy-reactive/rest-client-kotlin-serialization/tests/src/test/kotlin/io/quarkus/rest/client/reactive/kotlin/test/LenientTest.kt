package io.quarkus.rest.client.reactive.kotlin.test

import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.inject.Inject

class LenientTest {
    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withConfigurationResource("lenient.properties")
    }

    @Inject
    lateinit var json: Json

    @Test
    fun testLenient() {
        assertThat(json.decodeFromString<TestObject>("{\"name\":json}"))
            .isEqualTo(TestObject("json"))
    }

    @Serializable
    private data class TestObject(var name: String)
}
