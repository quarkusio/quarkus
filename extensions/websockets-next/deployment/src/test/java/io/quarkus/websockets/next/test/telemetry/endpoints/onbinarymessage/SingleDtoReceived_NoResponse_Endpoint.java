package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/received-single-dto-response-none")
public class SingleDtoReceived_NoResponse_Endpoint {

    @OnBinaryMessage
    public void onMessage(Dto dto) {
    }

}
