package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-none")
public class MultiTextReceived_NoResponse_Endpoint {

    @OnBinaryMessage
    public void onMessage(Multi<String> message) {

    }

}
