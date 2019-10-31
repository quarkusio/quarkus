package io.quarkus.security.runtime.interceptor.check;

import io.quarkus.security.identity.SecurityIdentity;

public interface SecurityCheck {
    void apply(SecurityIdentity identity);
}