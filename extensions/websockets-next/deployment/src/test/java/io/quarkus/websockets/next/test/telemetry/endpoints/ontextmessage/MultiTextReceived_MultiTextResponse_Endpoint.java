package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-multi-text")
public class MultiTextReceived_MultiTextResponse_Endpoint {

    @OnTextMessage
    public Multi<String> onMessage(Multi<String> messages) {
        return messages.flatMap(msg -> Multi.createFrom().items("echo 0: " + msg, "echo 1: " + msg));
    }

}
