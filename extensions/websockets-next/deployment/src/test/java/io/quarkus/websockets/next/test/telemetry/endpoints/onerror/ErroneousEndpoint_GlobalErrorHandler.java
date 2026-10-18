package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/server-error-global-handler")
public class ErroneousEndpoint_GlobalErrorHandler {

    private final AtomicInteger counter = new AtomicInteger();

    @OnTextMessage
    public Uni<String> onMessage(String txt) {
        return Uni.createFrom().failure(new IllegalArgumentException("echo " + counter.getAndIncrement() + ": " + txt));
    }

}
