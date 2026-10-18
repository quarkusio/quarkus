package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-single-text-response-multi-text")
public class SingleTextReceived_MultiTextResponse_Endpoint {

    @OnTextMessage
    public Multi<String> onMessage(String message) {
        return Multi.createFrom().items("echo 0: " + message, "echo 1: " + message);
    }

}
