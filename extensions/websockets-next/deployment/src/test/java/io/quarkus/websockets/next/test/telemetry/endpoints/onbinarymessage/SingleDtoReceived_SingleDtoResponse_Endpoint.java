package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-dto-response-single-dto")
public class SingleDtoReceived_SingleDtoResponse_Endpoint {

    @OnBinaryMessage
    public Dto onMessage(Dto dto) {
        return new Dto("echo 0: " + dto.property());
    }

}
