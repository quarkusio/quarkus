package io.quarkus.websockets.next.test.telemetry.endpoints.onbinarymessage;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/received-single-text-response-uni-text")
public class SingleTextReceived_UniTextResponse_Endpoint {

    @OnBinaryMessage
    public Uni<String> onMessage(String message) {
        return Uni.createFrom().item("echo 0: " + message);
    }

}
