package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.smallrye.mutiny.Uni;

@WebSocketClient(path = "/client-error-overloaded-on-error")
public class ErroneousClient_OverloadedOnError {

    public static CountDownLatch RUNTIME_EXCEPTION_LATCH = new CountDownLatch(1);
    public static CountDownLatch ILLEGAL_STATE_EXCEPTION_LATCH = new CountDownLatch(1);
    public static List<String> MESSAGES = new ArrayList<>();

    @OnTextMessage
    Uni<Void> onMessage(String message) {
        synchronized (this) {
            MESSAGES.add(message);
            if (MESSAGES.size() == 4) {
                return Uni.createFrom().failure(new RuntimeException("Expected error - 4 items"));
            }
            if (MESSAGES.size() == 8) {
                return Uni.createFrom().failure(new IllegalStateException("Expected error - 8 items"));
            }
            return Uni.createFrom().voidItem();
        }
    }

    @OnError
    public String onError(RuntimeException e) {
        RUNTIME_EXCEPTION_LATCH.countDown();
        return e.getMessage();
    }

    @OnError
    public void onError(IllegalStateException e) {
        ILLEGAL_STATE_EXCEPTION_LATCH.countDown();
    }
}
