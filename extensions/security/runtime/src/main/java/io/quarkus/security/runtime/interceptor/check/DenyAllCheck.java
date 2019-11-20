package io.quarkus.security.runtime.interceptor.check;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

public class DenyAllCheck implements SecurityCheck {

    public static final DenyAllCheck INSTANCE = new DenyAllCheck();

    private DenyAllCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}