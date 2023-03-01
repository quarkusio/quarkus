package io.quarkus.keycloak.pep.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.keycloak.pep.runtime.PolicyEnforcerResolver;
import io.quarkus.test.QuarkusUnitTest;

public class KeycloakAuthorizationOfflineEnforcerPatternTest {

    @RegisterExtension
    final static QuarkusUnitTest app = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AdminResource.class, PublicResource.class, UserResource.class)
                    .addAsResource("offline-enforcer-pattern-application.properties", "application.properties")
                    .addAsResource("quarkus-realm.json"));

    @Inject
    PolicyEnforcerResolver resolver;

    @Test
    public void testPathEnforcementForPathsWithPattern() {
        // we expect false even though /api/public and /api/open have disabled enforcement, because there is at
        // least one path with a pattern
        var defaultEnforcer = resolver.getPolicyEnforcer(null);
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/public/serve"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/public"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/users"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/users/me"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/open"));
        assertFalse(defaultEnforcer.isPathEnforcementDisabled("/api/unknown-url"));
    }
}
