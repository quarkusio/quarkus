package io.quarkus.it.oidc.dev.services;

import static io.quarkus.websockets.next.runtime.SecuritySupport.QUARKUS_IDENTITY_EXPIRE_TIME;

import jakarta.inject.Inject;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketSecurity;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/security-identity-update")
public class SecurityIdentityUpdateWebSocket {

    record ResponseDto(String message, String updatedIdentityPrincipal, String injectedIdentityBefore,
            boolean identitiesIdentical) {
    }

    record Metadata(String authorization) {
    }

    record RequestDto(String message, Metadata metadata) {
    }

    private volatile Long previousExpiresAt = null;

    @Inject
    WebSocketSecurity webSocketSecurity;

    @Inject
    SecurityIdentity injectedIdentity;

    @PermissionsAllowed("can-echo")
    @OnTextMessage
    Uni<ResponseDto> echo(RequestDto request) {
        String injectedIdentityBeforeUpdate = injectedIdentity.getPrincipal().getName();
        long expiresAtBeforeUpdate = injectedIdentity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME);
        if (request.metadata == null) {
            return Uni.createFrom().item(new ResponseDto(request.message, null, injectedIdentityBeforeUpdate, false));
        }
        return Uni.createFrom().emitter(emitter -> {
            webSocketSecurity
                    .updateSecurityIdentity(request.metadata.authorization)
                    .thenAccept(updatedIdentity -> {
                        long expiresAtAfterUpdate = injectedIdentity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME);
                        boolean identitiesIdentical = expiresAtAfterUpdate == expiresAtBeforeUpdate;
                        String updatedIdentityPrincipal = updatedIdentity.getPrincipal().getName();
                        emitter.complete(new ResponseDto(request.message, updatedIdentityPrincipal,
                                injectedIdentityBeforeUpdate, identitiesIdentical));
                    });
        });
    }

    @PermissionChecker("can-echo")
    boolean canEcho(RequestDto request, SecurityIdentity identity) {
        long currentExpiresAt = identity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME);
        if (previousExpiresAt == null) {
            previousExpiresAt = currentExpiresAt;
            return "hello".equals(request.message);
        }
        // this condition makes sure that `identity` represents updated SecurityIdentity
        if (currentExpiresAt < previousExpiresAt) {
            return false;
        } else {
            previousExpiresAt = currentExpiresAt;
        }
        return "hello".equals(request.message);
    }

}
