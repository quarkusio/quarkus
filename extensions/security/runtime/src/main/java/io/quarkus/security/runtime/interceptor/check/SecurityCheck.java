package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.identity.SecurityIdentity;

public interface SecurityCheck {
    void apply(SecurityIdentity identity, Method method, Object[] parameters);
}
