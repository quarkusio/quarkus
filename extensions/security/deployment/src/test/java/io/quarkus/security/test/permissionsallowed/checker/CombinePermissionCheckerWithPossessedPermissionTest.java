package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CombinePermissionCheckerWithPossessedPermissionTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    AdminOnlyMethodArgSecuredBean securedBean;

    @Test
    public void testAccessGrantedByPossessedPermissionAndChecker_allOf() {
        var adminWithSecuredPerm = new AuthData(ADMIN, true, new StringPermission("read", "secured"));
        var adminWithSecured2Perm = new AuthData(ADMIN, true, new StringPermission("read", "secured2"));

        assertSuccess(() -> securedBean.noSecurity("1", "2", 3, 4, 5), "noSecurity", USER_WITH_AUGMENTORS);
        assertSuccess(() -> securedBean.noSecurity("1", "2", 3, 4, 5), "noSecurity", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_allOf(1, 2, 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured2_allOf("1", "2", 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_allOf(1, 2, 3, 4, 5), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured2_allOf("1", "2", 3, 4, 5), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        assertSuccess(() -> securedBean.secured_allOf(1, 2, 3, 4, 5), "secured", adminWithSecuredPerm);
        assertSuccess(() -> securedBean.secured2_allOf("1", "2", 3, 4, 5), "secured2", adminWithSecured2Perm);
        // wrong value of the param 'one'
        assertFailureFor(() -> securedBean.secured2_allOf("9", "2", 3, 4, 5), ForbiddenException.class,
                adminWithSecured2Perm);
        // wrong value of the param 'five'
        assertFailureFor(() -> securedBean.secured2_allOf("1", "2", 3, 4, 6), ForbiddenException.class,
                adminWithSecured2Perm);
        // missing string permission "read:secured"
        assertFailureFor(() -> securedBean.secured_allOf(1, 2, 3, 4, 5), ForbiddenException.class,
                adminWithSecured2Perm);
        // missing string permission "read:secured2"
        assertFailureFor(() -> securedBean.secured2_allOf("1", "2", 3, 4, 5), ForbiddenException.class,
                adminWithSecuredPerm);
    }

    @Test
    public void testAccessGrantedByPossessedPermissionAndChecker_inclusiveAllOf() {
        var adminWithSecuredPerm = new AuthData(ADMIN, true, new StringPermission("read", "secured"));
        var adminWithSecured2Perm = new AuthData(ADMIN, true, new StringPermission("read", "secured2"));

        assertFailureFor(() -> securedBean.secured_inclusiveAllOf(1, 2, 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured2_inclusiveAllOf("1", "2", 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_inclusiveAllOf(1, 2, 3, 4, 5), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured2_inclusiveAllOf("1", "2", 3, 4, 5), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        assertSuccess(() -> securedBean.secured_inclusiveAllOf(1, 2, 3, 4, 5), "secured", adminWithSecuredPerm);
        assertSuccess(() -> securedBean.secured2_inclusiveAllOf("1", "2", 3, 4, 5), "secured2",
                adminWithSecured2Perm);
        // wrong value of the param 'one'
        assertFailureFor(() -> securedBean.secured2_inclusiveAllOf("9", "2", 3, 4, 5), ForbiddenException.class,
                adminWithSecured2Perm);
        // wrong value of the param 'five'
        assertFailureFor(() -> securedBean.secured2_inclusiveAllOf("1", "2", 3, 4, 6), ForbiddenException.class,
                adminWithSecured2Perm);
        // missing string permission "read:secured"
        assertFailureFor(() -> securedBean.secured_inclusiveAllOf(1, 2, 3, 4, 5), ForbiddenException.class,
                adminWithSecured2Perm);
        // missing string permission "read:secured2"
        assertFailureFor(() -> securedBean.secured2_inclusiveAllOf("1", "2", 3, 4, 5), ForbiddenException.class,
                adminWithSecuredPerm);
    }

    @Test
    public void testAccessGrantedByPossessedPermissionAndChecker_oneOf() {
        var adminWithSecuredPerm = new AuthData(ADMIN, true, new StringPermission("read", "secured"));
        var adminWithSecured2Perm = new AuthData(ADMIN, true, new StringPermission("read", "secured2"));

        assertFailureFor(() -> securedBean.secured_oneOf(1, 2, 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured2_oneOf("1", "2", 3, 4, 5), ForbiddenException.class,
                USER_WITH_AUGMENTORS);

        assertSuccess(() -> securedBean.secured_oneOf(1, 2, 3, 4, 5), "secured", adminWithSecuredPerm);
        assertSuccess(() -> securedBean.secured2_oneOf("1", "2", 3, 4, 5), "secured2", adminWithSecured2Perm);

        // wrong value of the param 'one', but has 'read:secured2'
        assertSuccess(() -> securedBean.secured2_oneOf("9", "2", 3, 4, 5), "secured2", adminWithSecured2Perm);
        // wrong value of the param 'five', but has 'read:secured2'
        assertSuccess(() -> securedBean.secured2_oneOf("1", "2", 3, 4, 6), "secured2", adminWithSecured2Perm);
        // wrong value of the param 'five' and no 'read:secured2'
        assertFailureFor(() -> securedBean.secured2_oneOf("1", "2", 3, 4, 16), ForbiddenException.class,
                adminWithSecuredPerm);

        // missing string permission "read:secured" and wrong param 'two'
        assertFailureFor(() -> securedBean.secured_oneOf(1, 4, 3, 4, 5), ForbiddenException.class,
                adminWithSecured2Perm);
        // has 'read:secured' but param '3' is wrong
        assertSuccess(() -> securedBean.secured_oneOf(1, 4, 6, 4, 5), "secured", adminWithSecuredPerm);
    }

    @ApplicationScoped
    public static class AdminOnlyMethodArgSecuredBean {

        @PermissionsAllowed(value = { "read:secured", "admin-only-method-arg-checker" }, inclusive = true)
        public String secured_inclusiveAllOf(int one, int two, int three, int ignored, int five) {
            return "secured";
        }

        @PermissionsAllowed(value = { "read:secured", "admin-only-method-arg-checker" })
        public String secured_oneOf(int one, int two, int three, int ignored, int five) {
            return "secured";
        }

        @PermissionsAllowed("read:secured")
        @PermissionsAllowed("admin-only-method-arg-checker")
        public String secured_allOf(int one, int two, int three, int ignored, int five) {
            return "secured";
        }

        public String noSecurity(String one, String two, int three, int ignored, int five) {
            return "noSecurity";
        }

        @PermissionsAllowed("read:secured2")
        @PermissionsAllowed("admin-only-method-arg-checker")
        public String secured2_allOf(String one, String two, int three, int ignored, int five) {
            return "secured2";
        }

        @PermissionsAllowed(value = { "read:secured2", "admin-only-method-arg-checker" }, inclusive = true)
        public String secured2_inclusiveAllOf(String one, String two, int three, int ignored, int five) {
            return "secured2";
        }

        @PermissionsAllowed(value = { "read:secured2", "admin-only-method-arg-checker" })
        public String secured2_oneOf(String one, String two, int three, int ignored, int five) {
            return "secured2";
        }
    }

    @Singleton
    public static class AdminOnlyMethodArgPermissionChecker {

        @PermissionChecker("admin-only-method-arg-checker")
        boolean canAccess(SecurityIdentity securityIdentity, Object three, Object one,
                Object five, Object two) {
            boolean methodArgsOk = equals(1, one) && equals(2, two) && equals(3, three) && equals(5, five);
            return methodArgsOk && !securityIdentity.isAnonymous() && "admin".equals(securityIdentity.getPrincipal().getName());
        }

        private static boolean equals(int expected, Object actual) {
            return expected == Integer.parseInt(actual.toString());
        }
    }
}
