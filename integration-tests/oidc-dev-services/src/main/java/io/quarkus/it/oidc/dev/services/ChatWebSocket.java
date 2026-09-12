package io.quarkus.it.oidc.dev.services;

import jakarta.inject.Inject;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@Authenticated
@WebSocket(path = "/chat/{username}")
public class ChatWebSocket {

    @Inject
    SecurityIdentity identity;

    @Inject
    SignalsResource signals;

    @OnOpen
    public String onOpen() {
        return "opened";
    }

    @OnTextMessage
    public String echo(String message) {
        String responseMessage = message + " " + identity.getPrincipal().getName();
        signals.fire("websockets:" + responseMessage, "admin");
        return responseMessage;
    }

}
