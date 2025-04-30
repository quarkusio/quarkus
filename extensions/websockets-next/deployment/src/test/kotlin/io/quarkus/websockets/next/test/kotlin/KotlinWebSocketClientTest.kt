package io.quarkus.websockets.next.test.kotlin

import io.quarkus.test.QuarkusUnitTest
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketClient
import io.quarkus.websockets.next.WebSocketConnector
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KotlinWebSocketClientTest {
    companion object {
        @RegisterExtension
        val test = QuarkusUnitTest()
            .withApplicationRoot { jar ->
                jar.addClasses(ServerEndpoint::class.java, ClientEndpoint::class.java)
            }
    }

    @Inject
    lateinit var connector: WebSocketConnector<ClientEndpoint>

    @TestHTTPResource("/")
    lateinit var uri: URI

    @Test
    fun test() {
        val connection = connector.baseUri(uri).connectAndAwait()
        connection.sendTextAndAwait("Hi!")

        assertTrue(ClientEndpoint.messagesLatch.await(5, TimeUnit.SECONDS))
        assertEquals("Hello there", ClientEndpoint.messages[0])
        assertEquals("Hi!", ClientEndpoint.messages[1])

        connection.closeAndAwait()
        assertTrue(ClientEndpoint.closedLatch.await(5, TimeUnit.SECONDS))
        assertTrue(ServerEndpoint.closedLatch.await(5, TimeUnit.SECONDS))
    }

    @WebSocket(path = "/endpoint")
    class ServerEndpoint {
        companion object {
            val closedLatch: CountDownLatch = CountDownLatch(1)
        }

        @OnOpen
        fun open(): String {
            return "Hello there"
        }

        @OnTextMessage
        fun echo(message: String): String {
            return message
        }

        @OnClose
        fun close() {
            closedLatch.countDown()
        }
    }

    @WebSocketClient(path = "/endpoint")
    class ClientEndpoint {
        companion object {
            val messages: MutableList<String> = CopyOnWriteArrayList()

            val messagesLatch: CountDownLatch = CountDownLatch(2)

            val closedLatch: CountDownLatch = CountDownLatch(1)
        }

        @OnTextMessage
        fun onMessage(message: String) {
            messages.add(message)
            messagesLatch.countDown()
        }

        @OnClose
        fun onClose() {
            closedLatch.countDown()
        }
    }
}
