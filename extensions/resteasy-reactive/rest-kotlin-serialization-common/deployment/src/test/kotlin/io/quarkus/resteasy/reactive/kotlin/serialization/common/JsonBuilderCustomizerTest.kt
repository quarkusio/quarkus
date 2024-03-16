package io.quarkus.resteasy.reactive.kotlin.serialization.common

import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.serializer
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.reflect.full.createType

class JsonBuilderCustomizerTest {

    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withApplicationRoot { jar: JavaArchive -> jar.addClasses(Greeting::class.java, HighPriorityCustomizer::class.java, LowPriorityCustomizer::class.java) }
            .withConfigurationResource("basic.properties")
    }

    @Inject
    lateinit var json: Json

    @Test
    fun testSerialization() {
        // prettyPrint should be set to false, because HighPriorityCustomizer overrides the value set in config
        Assertions.assertEquals("""{"name":"foo","message":"hello"}""",
            Arc.container().instance(Json::class.java).get().encodeToString(serializer(), Greeting("foo", "hello")))
    }

    @Test
    fun testDeserialization() {
        // isLenient should be set to true, because LowPriorityCustomizer overrides the value set in HighPriorityCustomizer
        Assertions.assertEquals(Greeting("foo", "hello"),
            json.decodeFromString(serializer(Greeting::class.createType()), """{name : "foo", "message" : "hello", "test": "dummy"}"""))
    }

    @Singleton
    @Priority(JsonBuilderCustomizer.DEFAULT_PRIORITY + 100) // this impl will be called before HighPriorityCustomizer
    class HighPriorityCustomizer : JsonBuilderCustomizer{

        @ExperimentalSerializationApi
        override fun customize(jsonBuilder: JsonBuilder) {
            jsonBuilder.prettyPrint = false
            jsonBuilder.isLenient = false
        }
    }

    @Singleton
    @Priority(JsonBuilderCustomizer.DEFAULT_PRIORITY + 10) // this impl will be called after HighPriorityCustomizer
    class LowPriorityCustomizer : JsonBuilderCustomizer{

        @ExperimentalSerializationApi
        override fun customize(jsonBuilder: JsonBuilder) {
            jsonBuilder.isLenient = true
        }
    }
}
