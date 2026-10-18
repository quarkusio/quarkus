package io.quarkus.websockets.next.test.kotlin

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import kotlinx.coroutines.delay

@WebSocket(path = "/echo-suspend")
class EchoSuspend {
    @OnTextMessage
    suspend fun process(msg: Message): Message {
        delay(100)
        return msg
    }

    @OnClose
    suspend fun close() {
        delay(100)
    }
}
