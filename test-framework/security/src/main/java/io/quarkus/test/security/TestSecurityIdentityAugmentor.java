package io.quarkus.test.security;

import java.lang.annotation.Annotation;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;

/**
 * Augments the {@code @TestSecurity} identity based on the test annotations, once before each test.
 * <p>
 * If the implementation also implements {@link SecurityIdentityAugmentor}, it is additionally applied
 * to the test identity on every HTTP request, which allows enforcing request-specific security
 * constraints that are otherwise bypassed by {@code @TestSecurity}.
 */
public interface TestSecurityIdentityAugmentor {
    SecurityIdentity augment(SecurityIdentity identity, Annotation[] annotations);
}
