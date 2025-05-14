package io.quarkus.websockets.next.runtime.spi.telemetry;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * The {@link io.quarkus.security.identity.request.AuthenticationRequest} used to for
 * update of the {@link io.quarkus.security.identity.SecurityIdentity} attached to a WebSocket connection.
 */
public final class WebSocketIdentityUpdateRequest extends BaseAuthenticationRequest {

    private final TokenCredential credential;

    public WebSocketIdentityUpdateRequest(TokenCredential credential) {
        this.credential = credential;
    }

    public TokenCredential getCredential() {
        return credential;
    }
}
