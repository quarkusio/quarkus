package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.request.AuthenticationRequest;

/**
 * A request to authenticate from a trusted source, such as an encrypted cookie
 *
 * TODO: move to quarkus-security
 */
public class TrustedAuthenticationRequest implements AuthenticationRequest {

    private final String principal;

    public TrustedAuthenticationRequest(String principal) {
        this.principal = principal;
    }

    public String getPrincipal() {
        return principal;
    }
}
