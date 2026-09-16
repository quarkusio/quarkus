package io.quarkus.it.oidc.dev.services;

import jakarta.annotation.security.RolesAllowed;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketSecurity;

@RolesAllowed("admin")
@WebSocket(path = "/change-in-updated-identity-roles")
public class UpdatedRoleSecurityIdentityWebSocket {

    private final WebSocketSecurity webSocketSecurity;

    UpdatedRoleSecurityIdentityWebSocket(WebSocketSecurity webSocketSecurity) {
        this.webSocketSecurity = webSocketSecurity;
    }

    @OnTextMessage
    String cheers(String accessToken) {
        webSocketSecurity.updateSecurityIdentity(accessToken);
        return "cheers";
    }

}
