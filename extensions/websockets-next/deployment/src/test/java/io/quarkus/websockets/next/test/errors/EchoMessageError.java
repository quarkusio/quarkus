package io.quarkus.websockets.next.test.errors;

import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/echo")
public class EchoMessageError {

    static final CountDownLatch MESSAGE_FAILURE_CALLED = new CountDownLatch(1);

    @OnTextMessage
    String echo(String message) {
        if ("foo".equals(message)) {
            MESSAGE_FAILURE_CALLED.countDown();
            throw new IllegalStateException("I cannot do it!");
        } else {
            return message;
        }
    }

}