package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/multi")
public class MultiEndpoint {

    @OnTextMessage
    Multi<String> echo(Multi<String> messages) {
        return messages.filter(msg -> !msg.startsWith("echo 0: "));
    }

}
