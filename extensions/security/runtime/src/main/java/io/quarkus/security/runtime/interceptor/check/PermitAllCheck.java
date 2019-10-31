package io.quarkus.security.runtime.interceptor.check;

import io.quarkus.security.identity.SecurityIdentity;

public class PermitAllCheck implements SecurityCheck {
    @Override
    public void apply(SecurityIdentity identity) {
    }
}