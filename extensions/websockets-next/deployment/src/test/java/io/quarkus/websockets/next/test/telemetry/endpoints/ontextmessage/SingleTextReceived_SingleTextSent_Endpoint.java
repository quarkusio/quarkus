package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/single-text-received-single-text-sent")
public class SingleTextReceived_SingleTextSent_Endpoint {

    @OnTextMessage
    public String onMessage(String message) {
        return "echo 0: " + message;
    }

}
