package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.smallrye.mutiny.Multi;

@WebSocketClient(path = "/multi")
public class MultiClient {

    @OnTextMessage
    Multi<String> echo(Multi<String> messages) {
        return messages.map(msg -> "echo 0: " + msg);
    }

}
