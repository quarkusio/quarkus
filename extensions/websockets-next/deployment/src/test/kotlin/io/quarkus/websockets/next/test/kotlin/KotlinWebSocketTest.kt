package io.quarkus.websockets.next.test.kotlin

import io.quarkus.test.QuarkusUnitTest
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.websockets.next.test.utils.WSClient
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI

class KotlinWebSocketTest {
    companion object {
        @RegisterExtension
        val test = QuarkusUnitTest()
            .withApplicationRoot { jar ->
                jar.addClasses(Echo::class.java, EchoSuspend::class.java, EchoUni::class.java,
                    BinaryEcho::class.java, BinaryEchoSuspend::class.java, BinaryEchoUni::class.java,
                    Message::class.java, WSClient::class.java)
            }
    }

    @Inject
    lateinit var vertx: Vertx

    @TestHTTPResource("echo")
    lateinit var echo: URI

    @TestHTTPResource("echo-suspend")
    lateinit var echoSuspend: URI

    @TestHTTPResource("echo-uni")
    lateinit var echoUni: URI

    @TestHTTPResource("binary-echo")
    lateinit var binaryEcho: URI

    @TestHTTPResource("binary-echo-suspend")
    lateinit var binaryEchoSuspend: URI

    @TestHTTPResource("binary-echo-uni")
    lateinit var binaryEchoUni: URI

    @Test
    fun testEcho() {
        doTest(echo)
    }

    @Test
    fun testEchoSuspend() {
        doTest(echoSuspend)
    }

    @Test
    fun testEchoUni() {
        doTest(echoUni)
    }

    private fun doTest(uri: URI) {
        WSClient.create(vertx).connect(uri).use { client ->
            val req = JsonObject().put("msg", "hello")
            val resp = client.sendAndAwaitReply(req.toString()).toJsonObject()
            assertEquals(req, resp)
        }
    }

    @Test
    fun testBinaryEcho() {
        doTestBinary(binaryEcho)
    }

    @Test
    fun testBinaryEchoSuspend() {
        doTestBinary(binaryEchoSuspend)
    }

    @Test
    fun testBinaryEchoUni() {
        doTestBinary(binaryEchoUni)
    }

    private fun doTestBinary(uri: URI) {
        WSClient.create(vertx).connect(uri).use { client ->
            val req = Buffer.buffer("hello there!")
            val resp = client.sendAndAwaitReply(req)
            assertEquals(req, resp)
        }
    }
}
