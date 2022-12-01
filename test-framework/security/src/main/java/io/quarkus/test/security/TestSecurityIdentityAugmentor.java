package io.quarkus.test.security;

import java.lang.annotation.Annotation;

import io.quarkus.security.identity.SecurityIdentity;

public interface TestSecurityIdentityAugmentor {
    SecurityIdentity augment(SecurityIdentity identity, Annotation[] annotations);
}
