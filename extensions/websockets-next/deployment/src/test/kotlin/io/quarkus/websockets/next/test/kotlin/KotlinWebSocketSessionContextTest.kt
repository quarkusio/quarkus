package io.quarkus.websockets.next.test.kotlin

import io.quarkus.test.QuarkusUnitTest
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.test.utils.WSClient
import io.vertx.core.Vertx
import jakarta.enterprise.context.SessionScoped
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import java.util.UUID

class KotlinWebSocketSessionContextTest {
    companion object {
        @RegisterExtension
        val test = QuarkusUnitTest()
            .withApplicationRoot { jar ->
                jar.addClasses(MyData::class.java, Endpoint::class.java, WSClient::class.java)
            }
    }

    @Inject
    lateinit var vertx: Vertx

    @TestHTTPResource("endpoint")
    lateinit var endpoint: URI

    @Test
    fun testEcho() {
        WSClient.create(vertx).connect(endpoint).use { client1 ->
            WSClient.create(vertx).connect(endpoint).use { client2 ->
                var id1: String? = null
                var id2: String? = null
                for (i in 1..10) {
                    val client = if (i % 2 == 0) client1 else client2
                    val req = "hello$i"
                    val resp = client.sendAndAwaitReply(req).toString().split(" ")
                    assertEquals(3, resp.size)
                    assertEquals(req, resp[0])
                    assertEquals(resp[1], resp[2])
                    if (i % 2 == 0) {
                        if (id1 == null) {
                            id1 = resp[1]
                        }
                        assertEquals(id1, resp[1])
                    } else {
                        if (id2 == null) {
                            id2 = resp[1]
                        }
                        assertEquals(id2, resp[1])
                    }
                }
                assertNotNull(id1)
                assertNotNull(id2)
                assertNotEquals(id1, id2)
            }
        }
    }

    @SessionScoped
    class MyData {
        val id = UUID.randomUUID().toString()
    }

    @WebSocket(path = "/endpoint")
    class Endpoint {
        @Inject
        lateinit var data: MyData

        @OnTextMessage
        suspend fun echo(message: String): String {
            val id1 = data.id
            delay(100)
            val id2 = data.id
            return "$message $id1 $id2"
        }
    }
}
