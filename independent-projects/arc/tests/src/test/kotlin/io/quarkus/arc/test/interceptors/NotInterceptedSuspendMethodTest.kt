package io.quarkus.arc.test.interceptors

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import jakarta.inject.Singleton
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NotInterceptedSuspendMethodTest {
    @RegisterExtension val container = ArcTestContainer(MyService::class.java)

    @Test
    fun test() {
        val service = Arc.container().instance(MyService::class.java).get()
        val result = runBlocking { service.hello() }

        assertEquals("hello", result)
    }

    // the class is not `open` and the method is not `open` either
    @Singleton
    class MyService {
        suspend fun hello(): String {
            delay(10)
            return "hello"
        }
    }
}
