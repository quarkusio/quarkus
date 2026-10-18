package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.smallrye.mutiny.Uni

@WebSocket(path = "/echo-uni")
class EchoUni {
    @OnTextMessage
    fun process(msg: Message): Uni<Message> {
        return Uni.createFrom().item(msg)
    }

    @OnClose
    fun close(): Uni<Void> {
        return Uni.createFrom().voidItem()
    }
}
