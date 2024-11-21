package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-text-response-none")
public class SingleTextReceived_NoResponse_Endpoint {

    @OnTextMessage
    public void onMessage(String message) {

    }

}
