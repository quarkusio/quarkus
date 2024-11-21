package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/single-text-received-single-text-sent")
public class SingleTextReceived_SingleTextSent_Endpoint {

    @OnBinaryMessage
    public String onMessage(String message) {
        return "echo 0: " + message;
    }

}
