package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ReactivePermissionCheckerTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    ReactivePermissionCheckerSecuredBean bean;

    @Test
    public void testCheckerAcceptingOnlySecurityIdentity() {
        assertSuccess(() -> bean.securityIdentityOnly(), "securityIdentityOnly", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securityIdentityOnly(), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securityIdentityOnly(), ForbiddenException.class, ADMIN);
    }

    @Test
    public void testCheckerAcceptingSecuredMethodArguments() {
        assertSuccess(() -> bean.securedMethodArguments(1, 2, 3), "securedMethodArguments", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securedMethodArguments(1, 2, 3), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.securedMethodArguments(1, 2, 3), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securedMethodArguments(1, 2, 3), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.securedMethodArguments(9, 2, 3), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securedMethodArguments(1, 9, 3), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.securedMethodArguments(1, 2, 9), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testPermissionCheckerRuntimeExceptionHandling() {
        assertSuccess(() -> bean.permissionCheckFailingForUser(), "permissionCheckFailingForUser", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.permissionCheckFailingForUser(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.permissionCheckFailingForUser(), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class ReactivePermissionCheckerSecuredBean {

        @PermissionsAllowed("security-identity-only")
        String securityIdentityOnly() {
            return "securityIdentityOnly";
        }

        @PermissionsAllowed("secured-method-args")
        String securedMethodArguments(int one, int two, int three) {
            return "securedMethodArguments";
        }

        @PermissionsAllowed("runtime-exception-for-user")
        String permissionCheckFailingForUser() {
            return "permissionCheckFailingForUser";
        }

    }

    @ApplicationScoped
    public static class ReactivePermissionChecker {

        @PermissionChecker("security-identity-only")
        Uni<Boolean> canAccess(SecurityIdentity identity) {
            return Uni.createFrom().item(identity.hasRole("admin"));
        }

        @PermissionChecker("secured-method-args")
        Uni<Boolean> canAccessWithArguments(SecurityIdentity identity, int one, int two, int three) {
            boolean isAdmin = identity.hasRole("admin");
            boolean argsOk = one == 1 && two == 2 && three == 3;
            return Uni.createFrom().item(isAdmin && argsOk);
        }

        @PermissionChecker("runtime-exception-for-user")
        Uni<Boolean> canAccessWithRuntimeException(SecurityIdentity identity) {
            if (identity.getPrincipal().getName().equals("user")) {
                return Uni.createFrom().failure(new RuntimeException("Expected runtime exception!"));
            }
            return Uni.createFrom().item(true);
        }
    }

}
