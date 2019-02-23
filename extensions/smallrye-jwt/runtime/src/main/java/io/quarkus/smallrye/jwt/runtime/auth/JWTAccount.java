package io.quarkus.smallrye.jwt.runtime.auth;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.undertow.security.idm.Account;

/**
 * Representation of the caller account using the JWTCallerPrincipal as an Undertow Account object.
 */
public class JWTAccount implements Account {
    private JsonWebToken principal;

    private Account delegate;

    public JWTAccount(JsonWebToken principal, Account delegate) {
        this.principal = principal;
        this.delegate = delegate;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return delegate.getRoles();
    }

}
