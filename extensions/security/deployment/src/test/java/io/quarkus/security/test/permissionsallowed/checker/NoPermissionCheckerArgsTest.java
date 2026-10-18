package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
import io.quarkus.test.QuarkusExtensionTest;

class NoPermissionCheckerArgsTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);
    private static final AuthData ANONYMOUS_WITH_AUGMENTORS = new AuthData(ANONYMOUS, true);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    void testAccessGranted() {
        assertSuccess(bean::securedBean, "success", ADMIN_WITH_AUGMENTORS);
    }

    @Test
    void testForbiddenException() {
        assertFailureFor(bean::securedBean, ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    void testUnauthorizedException() {
        assertFailureFor(bean::securedBean, UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(bean::securedBean, UnauthorizedException.class, ANONYMOUS_WITH_AUGMENTORS);
    }

    @Singleton
    static class SecuredBean {

        @PermissionsAllowed("some-value")
        String securedBean() {
            return "success";
        }

    }

    @Singleton
    static class Checker {

        @Inject
        SecurityIdentity identity;

        @PermissionChecker("some-value")
        boolean checkSomeValue() {
            if (identity.isAnonymous()) {
                throw new IllegalStateException("Anonymous identities cannot be granted access by the permission checker");
            }
            return identity.hasRole("admin");
        }

    }

}
