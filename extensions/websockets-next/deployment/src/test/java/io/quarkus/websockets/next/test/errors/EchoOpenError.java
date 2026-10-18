package io.quarkus.websockets.next.test.errors;

import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/echo")
public class EchoOpenError {

    static final CountDownLatch OPEN_CALLED = new CountDownLatch(1);

    @OnOpen
    void open() {
        OPEN_CALLED.countDown();
        throw new IllegalStateException("I cannot do it!");
    }

    @OnTextMessage
    String echo(String message) {
        return message;
    }

}