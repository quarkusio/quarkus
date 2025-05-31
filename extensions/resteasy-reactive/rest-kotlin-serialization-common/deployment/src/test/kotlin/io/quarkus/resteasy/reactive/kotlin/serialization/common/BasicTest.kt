package io.quarkus.resteasy.reactive.kotlin.serialization.common

import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.reflect.full.createType

class BasicTest {

    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withApplicationRoot { jar: JavaArchive -> jar.addClass(Greeting::class.java) }
            .withConfigurationResource("basic.properties")
    }

    @Test
    fun testSerialization() {
        // Aside from the CDI integration, this tests that the configured pretty-print is used
        Assertions.assertEquals(
"""{
    "name": "foo",
    "message": "hello"
}""",
            Arc.container().instance(Json::class.java).get().encodeToString(serializer(), Greeting("foo", "hello")))
    }

    @Test
    fun testDeserialization() {
        // Aside from the CDI integration, this tests that the configured ignore-unknown-keys is used
        Assertions.assertEquals(Greeting("foo", "hello"),
            Arc.container().instance(Json::class.java).get().decodeFromString(serializer(Greeting::class.createType()), """{"name" : "foo", "message" : "hello", "test": "dummy"}"""))
    }

}
