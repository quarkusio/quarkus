package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-single-text-response-multi-text")
public class SingleTextReceived_MultiTextResponse_Endpoint {

    @OnBinaryMessage
    public Multi<String> onMessage(String message) {
        return Multi.createFrom().items("echo 0: " + message, "echo 1: " + message);
    }

}
