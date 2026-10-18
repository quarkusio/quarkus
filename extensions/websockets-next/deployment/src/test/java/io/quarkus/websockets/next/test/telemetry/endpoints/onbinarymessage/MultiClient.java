package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.smallrye.mutiny.Multi;

@WebSocketClient(path = "/multi")
public class MultiClient {

    @OnBinaryMessage
    Multi<String> echo(Multi<String> messages) {
        return messages.map(msg -> "echo 0: " + msg);
    }

}
