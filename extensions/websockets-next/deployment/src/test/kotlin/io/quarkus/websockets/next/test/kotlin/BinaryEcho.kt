package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnBinaryMessage
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.WebSocket
import io.vertx.core.buffer.Buffer

@WebSocket(path = "/binary-echo")
class BinaryEcho {
    @OnBinaryMessage
    fun process(msg: Buffer): Buffer {
        return msg
    }

    @OnClose fun close() {}
}
