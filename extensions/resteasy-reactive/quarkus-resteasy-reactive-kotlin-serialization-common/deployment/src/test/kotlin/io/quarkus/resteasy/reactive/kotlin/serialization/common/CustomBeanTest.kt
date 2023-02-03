package io.quarkus.resteasy.reactive.kotlin.serialization.common

import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusUnitTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.reflect.full.createType

class CustomBeanTest {

    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withApplicationRoot { jar: JavaArchive -> jar.addClasses(Greeting::class.java, CustomJsonProducer::class.java) }
    }

    @Inject
    lateinit var json: Json

    @Test
    fun test() {
        Assertions.assertEquals(
"""{
 "name": "foo",
 "message": "hello"
}""",
            json.encodeToString(serializer(), Greeting("foo", "hello")))
    }

    @Singleton
    class CustomJsonProducer {

        @ExperimentalSerializationApi
        @Singleton
        @Produces
        fun customJson() = Json {
            prettyPrint = true
            prettyPrintIndent = " "
        }
    }
}
