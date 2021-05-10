package io.quarkus.test.security.common;

import io.quarkus.security.identity.SecurityIdentity;

public interface TestSecurityIdentityAugmentor {
    SecurityIdentity augment(SecurityIdentity identity);
}
