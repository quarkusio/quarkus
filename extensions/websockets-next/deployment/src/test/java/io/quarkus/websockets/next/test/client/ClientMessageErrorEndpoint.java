package io.quarkus.websockets.next.test.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;

@WebSocketClient(path = "/endpoint")
public class ClientMessageErrorEndpoint {

    static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(1);

    static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

    static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

    @OnTextMessage
    void message(String message) {
        if ("foo".equals(message)) {
            throw new IllegalStateException("I cannot do it!");
        } else {
            MESSAGES.add(message);
        }
        MESSAGE_LATCH.countDown();
    }

    @OnClose
    void close() {
        CLOSED_LATCH.countDown();
    }

}