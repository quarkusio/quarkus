
package io.quarkus.rest.client.reactive.kotlin.test

import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.inject.Inject

@OptIn(ExperimentalSerializationApi::class)
class PrettyPrintTest {
    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withConfigurationResource("pretty-print.properties")
    }

    @Inject
    lateinit var json: Json

    @Test
    fun testAlternateNames() {
        assertThat(json.encodeToString(TestObject("John Doe")))
            .isEqualTo("""
                {
                    "name": "John Doe"
                }""".trimIndent())
    }

    @Serializable
    private data class TestObject(@JsonNames("label") var name: String)
}
