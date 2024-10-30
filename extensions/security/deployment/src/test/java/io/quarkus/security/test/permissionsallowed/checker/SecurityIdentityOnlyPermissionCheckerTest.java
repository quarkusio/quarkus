package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityIdentityOnlyPermissionCheckerTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class,
                            SinglePermissionCheckerTargetBean.class, SecurityIdentityOnlyPermissionChecker.class));

    @Inject
    SinglePermissionCheckerTargetBean bean;

    @Test
    public void testSinglePermissionChecker() {
        assertSuccess(() -> bean.noSecurity(), "noSecurity", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.secured(), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.secured2(), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.noSecurity(), "noSecurity", USER);
        assertFailureFor(() -> bean.secured(), ForbiddenException.class, USER);
        assertFailureFor(() -> bean.secured2(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.noSecurity(), "noSecurity", ADMIN);
        assertFailureFor(() -> bean.secured(), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.secured2(), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.permissionWithoutChecker(), ForbiddenException.class, ADMIN);

        assertSuccess(() -> bean.noSecurity(), "noSecurity", ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> bean.secured(), "secured", ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> bean.secured2(), "secured2", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.permissionWithoutChecker(), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class SinglePermissionCheckerTargetBean {

        @PermissionsAllowed("security-identity-only")
        public String secured() {
            return "secured";
        }

        public String noSecurity() {
            return "noSecurity";
        }

        @PermissionsAllowed("security-identity-only")
        public String secured2() {
            return "secured2";
        }

        @PermissionsAllowed("permission-without-checker")
        public String permissionWithoutChecker() {
            return "permissionWithoutChecker";
        }
    }

}
