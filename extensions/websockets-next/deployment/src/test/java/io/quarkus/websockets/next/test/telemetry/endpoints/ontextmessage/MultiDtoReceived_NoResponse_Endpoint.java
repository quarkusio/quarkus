package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-dto-response-none")
public class MultiDtoReceived_NoResponse_Endpoint {

    @OnTextMessage
    public void onMessage(Multi<Dto> dto) {

    }

}
