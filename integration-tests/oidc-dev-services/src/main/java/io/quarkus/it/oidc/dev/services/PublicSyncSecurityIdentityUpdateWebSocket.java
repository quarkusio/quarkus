package io.quarkus.it.oidc.dev.services;

import static io.quarkus.websockets.next.runtime.SecuritySupport.QUARKUS_IDENTITY_EXPIRE_TIME;

import jakarta.inject.Inject;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketSecurity;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/public/sync-security-identity-update")
public class PublicSyncSecurityIdentityUpdateWebSocket {

    @Inject
    WebSocketSecurity webSocketSecurity;

    @Inject
    SecurityIdentity securityIdentity;

    @OnTextMessage
    IdentityUpdateResponse echo(SecurityIdentityUpdateWebSocket.RequestDto request) {
        if (request.metadata() == null || request.metadata().authorization() == null) {
            return new IdentityUpdateResponse(request.message(), securityIdentity.getPrincipal().getName(),
                    securityIdentity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME));
        }
        return webSocketSecurity
                .updateSecurityIdentity(request.metadata().authorization())
                .thenApply(updatedIdentity -> {
                    String updatedIdentityPrincipal = updatedIdentity.getPrincipal().getName();
                    return new IdentityUpdateResponse(request.message(), updatedIdentityPrincipal,
                            updatedIdentity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME));
                })
                .toCompletableFuture().join();
    }

    @OnError
    Uni<Void> closeOnError(Exception e, WebSocketConnection connection) {
        var rootCause = ExceptionUtil.getRootCause(e);
        return connection.close(new CloseReason(1008, rootCause.getClass().getName()));
    }
}
