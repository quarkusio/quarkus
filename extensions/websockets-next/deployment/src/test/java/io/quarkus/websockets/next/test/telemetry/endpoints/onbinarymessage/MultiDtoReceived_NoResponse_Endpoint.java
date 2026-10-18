package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-dto-response-none")
public class MultiDtoReceived_NoResponse_Endpoint {

    @OnBinaryMessage
    public void onMessage(Multi<Dto> dto) {

    }

}
