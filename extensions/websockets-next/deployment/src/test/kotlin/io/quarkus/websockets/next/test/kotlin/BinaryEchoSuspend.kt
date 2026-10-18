package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnBinaryMessage
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.WebSocket
import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.delay

@WebSocket(path = "/binary-echo-suspend")
class BinaryEchoSuspend {
    @OnBinaryMessage
    suspend fun process(msg: Buffer): Buffer {
        delay(100)
        return msg
    }

    @OnClose
    suspend fun close() {
        delay(100)
    }
}
