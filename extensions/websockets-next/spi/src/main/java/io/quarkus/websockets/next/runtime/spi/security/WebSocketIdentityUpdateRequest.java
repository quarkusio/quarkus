package io.quarkus.websockets.next.runtime.spi.security;

import java.util.Objects;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * The {@link io.quarkus.security.identity.request.AuthenticationRequest} used to for
 * update of the {@link io.quarkus.security.identity.SecurityIdentity} attached to a WebSocket connection.
 */
public final class WebSocketIdentityUpdateRequest extends BaseAuthenticationRequest {

    private final TokenCredential credential;
    private final SecurityIdentity currentSecurityIdentity;

    public WebSocketIdentityUpdateRequest(TokenCredential credential, SecurityIdentity currentSecurityIdentity) {
        this.credential = Objects.requireNonNull(credential);
        this.currentSecurityIdentity = Objects.requireNonNull(currentSecurityIdentity);
    }

    public TokenCredential getCredential() {
        return credential;
    }

    public SecurityIdentity getCurrentSecurityIdentity() {
        return currentSecurityIdentity;
    }
}
