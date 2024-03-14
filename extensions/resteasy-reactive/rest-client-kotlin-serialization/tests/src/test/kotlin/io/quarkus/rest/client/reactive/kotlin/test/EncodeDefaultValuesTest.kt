package io.quarkus.rest.client.reactive.kotlin.test

import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.inject.Inject

class EncodeDefaultValuesTest {
    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withConfigurationResource("encode-default-values.properties")
    }

    @Inject
    lateinit var json: Json

    @Test
    fun testNoDefaults() {
        assertThat(json.encodeToString(TestObject()))
            .isEqualTo("{}")
    }
    @Test
    fun testExplicitNulls() {
        assertThat(json.encodeToString(TestObject(blank = null)))
            .isEqualTo("{\"blank\":null}")
    }

    @Serializable
    private class TestObject(var name: String = "Default Value", var blank: String? = "")
}
