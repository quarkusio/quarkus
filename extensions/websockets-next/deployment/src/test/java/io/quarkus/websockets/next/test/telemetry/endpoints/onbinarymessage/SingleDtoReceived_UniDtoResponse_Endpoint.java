package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/received-single-dto-response-uni-dto")
public class SingleDtoReceived_UniDtoResponse_Endpoint {

    @OnBinaryMessage
    public Uni<Dto> onMessage(Dto dto) {
        return Uni.createFrom().item("echo 0: " + dto.property()).map(Dto::new);
    }

}
