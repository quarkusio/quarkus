package io.quarkus.security.runtime.interceptor.check;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

public class AuthenticatedCheck implements SecurityCheck {

    public static final AuthenticatedCheck INSTANCE = new AuthenticatedCheck();

    private AuthenticatedCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }
    }
}
