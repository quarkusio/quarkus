package io.quarkus.vertx.http.security.token;

import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * The {@link io.quarkus.security.identity.request.AuthenticationRequest} used for one-time authentication token credentials.
 */
public final class OneTimeTokenAuthenticationRequest extends BaseAuthenticationRequest {

    private final PasswordCredential oneTimeAuthenticationToken;

    private OneTimeTokenAuthenticationRequest(PasswordCredential oneTimeAuthenticationToken) {
        this.oneTimeAuthenticationToken = oneTimeAuthenticationToken;
    }

    private OneTimeTokenAuthenticationRequest(char[] oneTimeAuthenticationToken) {
        this(new PasswordCredential(oneTimeAuthenticationToken));
    }

    public OneTimeTokenAuthenticationRequest(String oneTimeAuthenticationToken) {
        this(oneTimeAuthenticationToken.toCharArray());
    }

    public PasswordCredential getCredential() {
        return oneTimeAuthenticationToken;
    }
}
