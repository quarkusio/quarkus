package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnBinaryMessage
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.WebSocket
import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.delay

@WebSocket(path = "/binary-echo-uni")
class BinaryEchoUni {
    @OnBinaryMessage
    fun process(msg: Buffer): Uni<Buffer> {
        return Uni.createFrom().item(msg)
    }


    @OnClose
    fun close(): Uni<Void> {
        return Uni.createFrom().voidItem()
    }
}
