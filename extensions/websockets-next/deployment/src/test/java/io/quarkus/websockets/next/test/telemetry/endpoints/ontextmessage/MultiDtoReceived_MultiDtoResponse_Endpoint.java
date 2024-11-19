package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/received-multi-dto-response-multi-dto")
public class MultiDtoReceived_MultiDtoResponse_Endpoint {

    @OnTextMessage
    public Multi<Dto> onMessage(Multi<Dto> messages) {
        return messages
                .map(Dto::property)
                .flatMap(msg -> Multi.createFrom().items("echo 0: " + msg, "echo 1: " + msg))
                .map(Dto::new);
    }

}
