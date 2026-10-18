package io.quarkus.it.oidc.dev.services;

import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketSecurity;

@Authenticated
@WebSocket(path = "/expired-updated-identity")
public class ExpiredUpdatedSecurityIdentityWebSocket {

    private final WebSocketSecurity webSocketSecurity;

    ExpiredUpdatedSecurityIdentityWebSocket(WebSocketSecurity webSocketSecurity) {
        this.webSocketSecurity = webSocketSecurity;
    }

    @OnTextMessage
    String bye(String accessToken) {
        webSocketSecurity.updateSecurityIdentity(accessToken);
        return "bye";
    }

}
