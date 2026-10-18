package io.quarkus.it.keycloak.websockets.tenantannotation;

import io.quarkus.oidc.Tenant;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@Authenticated
@Tenant("hr")
@WebSocket(path = "/ws/tenant-annotation/hr-tenant")
public class WebSocketEndpointWithTenantAnnotationAndAuthenticated {

    @OnOpen
    String open() {
        return "ready";
    }

    @OnTextMessage
    String echo(String message) {
        return "hr-tenant echo: " + message;
    }

}
