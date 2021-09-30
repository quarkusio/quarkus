package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class DenyAllCheck implements SecurityCheck {

    public static final DenyAllCheck INSTANCE = new DenyAllCheck();

    private DenyAllCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        doApply(identity);
    }

    @Override
    public void apply(SecurityIdentity identity, MethodDescription method, Object[] parameters) {
        doApply(identity);
    }

    private void doApply(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}
