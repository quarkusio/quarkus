package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/broadcast")
public class BroadcastingEndpoint {

    @OnTextMessage(broadcast = true)
    public String onMessage(String message) {
        return "echo 0: " + message;
    }

}
