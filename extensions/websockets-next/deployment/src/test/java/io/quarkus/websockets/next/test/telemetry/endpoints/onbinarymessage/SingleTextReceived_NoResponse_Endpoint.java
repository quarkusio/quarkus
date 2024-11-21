package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-text-response-none")
public class SingleTextReceived_NoResponse_Endpoint {

    @OnBinaryMessage
    public void onMessage(String message) {

    }

}
