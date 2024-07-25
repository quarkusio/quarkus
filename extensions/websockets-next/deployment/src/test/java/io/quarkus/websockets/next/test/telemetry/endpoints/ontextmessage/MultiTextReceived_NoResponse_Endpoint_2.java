package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-none-2")
public class MultiTextReceived_NoResponse_Endpoint_2 {

    @OnTextMessage
    public void onMessage(Multi<String> message) {

    }

}
