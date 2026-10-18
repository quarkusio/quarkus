package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/bounce")
public class BounceEndpoint {

    @OnBinaryMessage
    public String onMessage(String message) {
        return "echo 0: " + message;
    }

}
