package io.quarkus.security.webauthn;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.vertx.ext.auth.webauthn.WebAuthnCredentials;

public class WebAuthnAuthenticationRequest extends BaseAuthenticationRequest {

    private WebAuthnCredentials credentials;

    public WebAuthnAuthenticationRequest(WebAuthnCredentials credentials) {
        this.credentials = credentials;
    }

    public WebAuthnCredentials getCredentials() {
        return credentials;
    }

}
