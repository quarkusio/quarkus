package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;

@WebSocketClient(path = "/client-endpoint-with-path-param/{name}")
public class ClientEndpointWithPathParams {

    @OnTextMessage
    public void onTextMessage(String message) {
    }

}
