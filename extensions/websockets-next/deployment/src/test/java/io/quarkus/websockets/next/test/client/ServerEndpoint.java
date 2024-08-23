package io.quarkus.websockets.next.test.client;

import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/endpoint")
public class ServerEndpoint {

    static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

    @OnTextMessage
    String echo(String message) {
        return message;
    }

    @OnClose
    void close() {
        CLOSED_LATCH.countDown();
    }

}