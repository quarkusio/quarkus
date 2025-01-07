package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket

@WebSocket(path = "/echo")
class Echo {
    @OnTextMessage
    fun process(msg: Message): Message {
        return msg
    }

    @OnClose
    fun close() {
    }
}
