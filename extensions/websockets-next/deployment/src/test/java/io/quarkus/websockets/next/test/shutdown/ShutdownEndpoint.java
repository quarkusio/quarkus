package io.quarkus.websockets.next.test.shutdown;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/shutdown")
public class ShutdownEndpoint {

    @OnOpen
    String open() {
        return "open";
    }

}
