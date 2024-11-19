package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/ping/{one}/and/{two}")
public class PingEndpoint {

    @OnOpen
    String process(@PathParam String one, @PathParam String two) {
        return one + ":" + two;
    }

}
