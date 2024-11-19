package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/server-error-on-close")
public class ErroneousServerEndpoint_OnClose {

    public static CountDownLatch ILLEGAL_STATE_EXCEPTION_LATCH = new CountDownLatch(1);

    @OnBinaryMessage
    public Multi<String> onMessage(String message) {
        return Multi.createFrom().items("Bobby");
    }

    @OnClose
    public void onClose() {
        throw new IllegalStateException("Expected exception");
    }

    @OnError
    public void onError(IllegalStateException e) {
        ILLEGAL_STATE_EXCEPTION_LATCH.countDown();
    }
}
