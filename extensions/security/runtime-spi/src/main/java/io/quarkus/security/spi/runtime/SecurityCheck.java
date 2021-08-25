package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;

import io.quarkus.security.identity.SecurityIdentity;

public interface SecurityCheck {
    void apply(SecurityIdentity identity, Method method, Object[] parameters);
}
