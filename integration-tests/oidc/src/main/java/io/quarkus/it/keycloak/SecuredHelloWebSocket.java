package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

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
