package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-text-response-single-text")
public class MultiTextReceived_SingleTextResponse_Endpoint {

    @OnBinaryMessage
    public String onMessage(Multi<String> message) {
        return "Alpha Shallows";
    }

}
