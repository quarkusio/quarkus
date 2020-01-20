package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.interceptor.check.SecurityCheck;

public class AnonymousCheck implements SecurityCheck {

    public static final AnonymousCheck INSTANCE = new AnonymousCheck();

    private AnonymousCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (!identity.isAnonymous()) {
            throw new ForbiddenException();
        }
    }
}
