package io.quarkus.websockets.next.runtime.spi.telemetry;

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
    private final SecurityIdentity previousSecurityIdentity;

    public WebSocketIdentityUpdateRequest(TokenCredential credential, SecurityIdentity previousSecurityIdentity) {
        this.credential = Objects.requireNonNull(credential);
        this.previousSecurityIdentity = Objects.requireNonNull(previousSecurityIdentity);
    }

    public TokenCredential getCredential() {
        return credential;
    }

    public SecurityIdentity getPreviousSecurityIdentity() {
        return previousSecurityIdentity;
    }
}
