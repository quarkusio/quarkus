package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

import io.quarkus.security.identity.SecurityIdentity;

@ServerEndpoint("/secured-hello")
@ApplicationScoped
@RolesAllowed("user")
public class SecuredHelloWebSocket {

    @Inject
    SecurityIdentity identity;

    @OnMessage
    public String onMessage(String message) {
        return message + " " + identity.getPrincipal().getName();
    }
}
