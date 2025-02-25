package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-dto-response-none")
public class SingleDtoReceived_NoResponse_Endpoint {

    @OnTextMessage
    public void onMessage(Dto dto) {
    }

}
