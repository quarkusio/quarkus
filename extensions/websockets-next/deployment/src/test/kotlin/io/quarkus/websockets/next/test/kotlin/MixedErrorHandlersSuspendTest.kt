package io.quarkus.websockets.next.test.kotlin

import io.quarkus.test.QuarkusUnitTest
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.websockets.next.OnError
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.test.utils.WSClient
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI

class MixedErrorHandlersSuspendTest {
    companion object {
        @RegisterExtension
        val test: QuarkusUnitTest? = QuarkusUnitTest()
            .withApplicationRoot { jar ->
                jar.addClasses(Echo::class.java, GlobalErrorHandlers::class.java, WSClient::class.java)
            }
    }

    @Inject
    lateinit var vertx: Vertx

    @TestHTTPResource("echo")
    lateinit var testUri: URI

    @Test
    fun test() {
        val client = WSClient.create(vertx).connect(testUri)

        client.send("0")
        assertEquals("OK", client.waitForNextMessage().toString())

        client.send("1")
        assertEquals("IAE", client.waitForNextMessage().toString())

        client.send("2")
        assertEquals("ISE", client.waitForNextMessage().toString())

        client.send("3")
        assertEquals("RE", client.waitForNextMessage().toString())

        client.send("4")
        assertEquals("E", client.waitForNextMessage().toString())

        client.send("5")
        assertEquals("OK", client.waitForNextMessage().toString())
    }

    @WebSocket(path = "/echo")
    class Echo {
        @OnTextMessage
        suspend fun echo(msg: String): String {
            delay(100)
            if ("1" == msg) {
                throw IllegalArgumentException()
            } else if ("2" == msg) {
                throw IllegalStateException()
            } else if ("3" == msg) {
                throw RuntimeException()
            } else if ("4" == msg) {
                throw Exception()
            } else {
                return "OK"
            }
        }

        @OnError
        suspend fun handle(e: IllegalArgumentException): String {
            delay(100)
            return "IAE"
        }

        @OnError
        suspend fun handle(e: RuntimeException): String {
            delay(100)
            return "RE"
        }
    }

    @ApplicationScoped
    class GlobalErrorHandlers {
        @OnError
        suspend fun handle(e: IllegalStateException): String {
            delay(100)
            return "ISE"
        }

        @OnError
        suspend fun handle(e: Exception): String {
            delay(100)
            return "E"
        }
    }
}
