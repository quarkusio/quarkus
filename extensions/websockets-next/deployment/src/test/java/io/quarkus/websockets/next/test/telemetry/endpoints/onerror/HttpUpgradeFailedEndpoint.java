package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/http-upgrade-failed")
public class HttpUpgradeFailedEndpoint {

    @OnTextMessage
    public Uni<Dto> onMessage(Multi<Dto> dto) {
        return dto.toUni();
    }

}
