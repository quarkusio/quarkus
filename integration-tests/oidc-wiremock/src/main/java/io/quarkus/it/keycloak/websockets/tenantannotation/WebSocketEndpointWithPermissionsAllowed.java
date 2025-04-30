package io.quarkus.it.keycloak.websockets.tenantannotation;

import io.quarkus.oidc.Tenant;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@PermissionsAllowed("bob")
@Tenant("hr")
@WebSocket(path = "/ws/tenant-annotation/permissions-allowed")
public class WebSocketEndpointWithPermissionsAllowed {

    @OnOpen
    String open() {
        return "ready";
    }

    @OnTextMessage
    String echo(String message) {
        return "permissions-allowed echo: " + message;
    }

    @PermissionChecker("bob")
    boolean isBob(SecurityIdentity securityIdentity) {
        return securityIdentity.getPrincipal().getName().equals("bob");
    }
}
