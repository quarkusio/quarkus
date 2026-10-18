package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-single-text")
public class MultiTextReceived_SingleTextResponse_Endpoint {

    @OnTextMessage
    public String onMessage(Multi<String> message) {
        return "Alpha Shallows";
    }

}
