package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-dto-response-single-dto")
public class MultiDtoReceived_SingleDtoResponse_Endpoint {

    @OnTextMessage
    public String onMessage(Multi<String> message) {
        return "ut labore et dolore magna aliqua";
    }

}
