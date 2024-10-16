package io.quarkus.test.security;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * This test mechanism is fallback when no other mechanism manages to authenticate.
 * When the test method is annotated with the {@link TestSecurity} annotation,
 * users can still send credentials inside HTTP request and the credentials will have priority.
 */
@ApplicationScoped
public class FallbackTestHttpAuthenticationMechanism extends AbstractTestHttpAuthenticationMechanism {

}
