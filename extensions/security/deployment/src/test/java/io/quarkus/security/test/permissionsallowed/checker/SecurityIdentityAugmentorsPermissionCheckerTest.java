package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class SecurityIdentityAugmentorsPermissionCheckerTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    /**
     * Tests that {@link SecurityIdentity} passed to the {@link PermissionChecker} methods is augmented by all the
     * augmentors (because that's the last operation we do on the identity, then it's de facto final).
     */
    @Test
    public void testPermissionCheckerUsesAugmentedIdentity() {
        assertSuccess(bean::securedMethod, "secured", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(bean::securedMethod, ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class SecuredBean {

        @PermissionsAllowed("canCallSecuredMethod")
        String securedMethod() {
            return "secured";
        }

        @PermissionChecker("canCallSecuredMethod")
        boolean canCallSecuredMethod(SecurityIdentity identity) {
            if (!identity.hasRole("lowest-priority-augmentor")) {
                Log.error("Role granted by the augmentor with the smallest priority is missing");
                return false;
            }
            if (!identity.hasRole("default-priority-augmentor")) {
                Log.error("Role granted by the augmentor with a default priority is missing");
                return false;
            }
            if (!identity.hasRole("highest-priority-augmentor")) {
                Log.error("Role granted by the augmentor with the highest priority is missing");
                return false;
            }
            return "admin".equals(identity.getPrincipal().getName());
        }
    }

    @ApplicationScoped
    public static class AugmentorWithLowestPriority implements SecurityIdentityAugmentor {

        @Override
        public int priority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(
                    QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addRole("lowest-priority-augmentor")
                            .build());
        }
    }

    @ApplicationScoped
    public static class AugmentorWithDefaultPriority implements SecurityIdentityAugmentor {

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(
                    QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addRole("default-priority-augmentor")
                            .build());
        }
    }

    @ApplicationScoped
    public static class AugmentorWithHighestPriority implements SecurityIdentityAugmentor {

        @Override
        public int priority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(
                    QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addRole("highest-priority-augmentor")
                            .build());
        }
    }
}
