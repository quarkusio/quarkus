package io.quarkus.websockets.test;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;

@ClientEndpoint
public class TestWebSocketClient {

    private final LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();

    @OnMessage
    void echo(String msg) {
        messages.add(msg);
    }

    public String get() throws InterruptedException {
        return messages.pollFirst(10, TimeUnit.SECONDS);
    }
}
