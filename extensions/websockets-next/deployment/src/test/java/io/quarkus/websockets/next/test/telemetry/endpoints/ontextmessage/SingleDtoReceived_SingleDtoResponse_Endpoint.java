package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-dto-response-single-dto")
public class SingleDtoReceived_SingleDtoResponse_Endpoint {

    @OnTextMessage
    public Dto onMessage(Dto dto) {
        return new Dto("echo 0: " + dto.property());
    }

}
