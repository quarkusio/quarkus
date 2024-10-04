package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.smallrye.mutiny.Uni;

@WebSocketClient(path = "/client-error-no-on-error")
public class ErroneousClient_NoOnError {

    public static List<String> MESSAGES = new ArrayList<>();

    @OnTextMessage
    Uni<Void> onMessage(String message) {
        synchronized (this) {
            MESSAGES.add(message);
            if (MESSAGES.size() == 4) {
                return Uni.createFrom().failure(new RuntimeException("You asked for an error, you got the error!"));
            }
            return Uni.createFrom().voidItem();
        }
    }

}
