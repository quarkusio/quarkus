package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;

@WebSocketClient(path = "/bounce", clientId = "bounce-client-id")
public class BounceClient {

    public static List<String> MESSAGES = new CopyOnWriteArrayList<>();
    public static CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

    @OnTextMessage
    void echo(String message) {
        MESSAGES.add(message);
    }

    @OnClose
    void onClose() {
        CLOSED_LATCH.countDown();
    }

}
