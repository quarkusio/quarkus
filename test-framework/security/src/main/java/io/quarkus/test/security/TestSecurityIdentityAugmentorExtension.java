package io.quarkus.test.security;

import io.quarkus.security.identity.SecurityIdentityAugmentor;

/**
 * A {@link SecurityIdentityAugmentor} contributed by a test framework extension that
 * {@code @TestSecurity} applies automatically, without the user having to list it in
 * {@code @TestSecurity#augmentors}.
 * <p>
 * Regular {@link SecurityIdentityAugmentor} CDI beans are not applied during a
 * {@code @TestSecurity} test, so augmentors needing a database or external systems do not run.
 * Implementing this interface is how a test framework extension opts its augmentor into the test.
 */
public interface TestSecurityIdentityAugmentorExtension extends SecurityIdentityAugmentor {
}
