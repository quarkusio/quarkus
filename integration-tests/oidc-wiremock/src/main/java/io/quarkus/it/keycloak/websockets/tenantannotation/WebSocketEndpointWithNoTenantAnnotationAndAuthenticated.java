package io.quarkus.it.keycloak.websockets.tenantannotation;

import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@Authenticated
@WebSocket(path = "/ws/tenant-annotation/no-annotation")
public class WebSocketEndpointWithNoTenantAnnotationAndAuthenticated {

    @OnOpen
    String open() {
        return "ready";
    }

    @OnTextMessage
    String echo(String message) {
        return "no-annotation echo: " + message;
    }

}
