package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class AuthenticatedCheck implements SecurityCheck {

    public static final AuthenticatedCheck INSTANCE = new AuthenticatedCheck();

    private AuthenticatedCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }
    }
}
