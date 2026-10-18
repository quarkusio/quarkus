package io.quarkus.test.security.callback;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.security.TestIdentityAssociation;
import io.smallrye.mutiny.Uni;

@Tag("https://github.com/quarkusio/quarkus/issues/36601")
/*
 * This set of tests are testing QuarkusSecurityTestExtension callbacks @BeforeEach and @AfterEach
 * These should not interfere with CDI in when tests are not annotated with @TestSecurity
 * Way to measure if these callbacks are doing something is by testing TestIdentity in
 * CDI.current().select(TestIdentityAssociation.class)
 * This should be set to value in BeforeEach and then set to null in AfterEach in case, @TestSecurity is present
 * and not touched otherwise.
 * To properly test handling in these methods, this test package uses a testClass for every testCase,
 * to isolate calls of *Each methods
 */
abstract public class AbstractSecurityCallbackTest {
    protected static final SecurityIdentity MOCK_SECURITY_IDENTITY = new SecurityIdentity() {
        @Override
        public Principal getPrincipal() {
            return () -> "";
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasRole(String role) {
            return false;
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> credentialType) {
            return null;
        }

        @Override
        public Set<Credential> getCredentials() {
            return Collections.emptySet();
        }

        @Override
        public Set<Permission> getPermissions() {
            return Collections.emptySet();
        }

        @Override
        public <T> T getAttribute(String name) {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            return Uni.createFrom().item(false);
        }
    };

    protected static void assertTestIdentityIsNull() {
        Assertions.assertNull(CDI.current().select(TestIdentityAssociation.class).get().getTestIdentity(),
                "TestIdentity should be null");
    }

    protected static void assertTestIdentityIsNotNull() {
        Assertions.assertNotNull(CDI.current().select(TestIdentityAssociation.class).get().getTestIdentity(),
                "TestIdentity should have value");
    }

    protected static void setTestIdentityToValue() {
        CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(MOCK_SECURITY_IDENTITY);
    }

    protected static void setTestIdentityToNull() {
        CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(null);
    }
}
