package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.identity.SecurityIdentity;

public class PermitAllCheck implements SecurityCheck {

    public static final PermitAllCheck INSTANCE = new PermitAllCheck();

    private PermitAllCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
    }
}