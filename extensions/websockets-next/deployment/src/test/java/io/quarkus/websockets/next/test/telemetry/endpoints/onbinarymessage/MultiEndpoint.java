package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/multi")
public class MultiEndpoint {

    @OnBinaryMessage
    Multi<String> echo(Multi<String> messages) {
        return messages.filter(msg -> !msg.startsWith("echo 0: "));
    }

}
