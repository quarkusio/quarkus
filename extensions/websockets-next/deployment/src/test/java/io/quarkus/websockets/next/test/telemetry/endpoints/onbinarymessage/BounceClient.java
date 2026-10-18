package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocketClient;

@WebSocketClient(path = "/bounce")
public class BounceClient {

    @OnBinaryMessage
    void echo(String message) {
    }

}
