package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-multi-text")
public class MultiTextReceived_MultiTextResponse_Endpoint {

    @OnBinaryMessage
    public Multi<String> onMessage(Multi<String> messages) {
        return messages.flatMap(msg -> Multi.createFrom().items("echo 0: " + msg, "echo 1: " + msg));
    }

}
