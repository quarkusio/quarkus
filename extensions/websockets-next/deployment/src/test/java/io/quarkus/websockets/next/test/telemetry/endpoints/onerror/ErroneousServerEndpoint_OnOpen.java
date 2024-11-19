package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/server-error-on-open")
public class ErroneousServerEndpoint_OnOpen {

    @OnOpen
    public Uni<Void> onOpen() {
        return Uni.createFrom().failure(new IllegalStateException("Expected failure"));
    }

    @OnTextMessage
    public void onMessage(String message) {
    }

}
