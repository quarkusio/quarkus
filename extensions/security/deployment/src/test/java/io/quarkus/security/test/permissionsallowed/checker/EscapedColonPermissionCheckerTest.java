package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

class EscapedColonPermissionCheckerTest {

    private static final String EC = "\\:";

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    void checkerMatchesRawEscapedValueInName() {
        assertSuccess(() -> bean.checkerEscapedName(true), "checkerEscapedName", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.checkerEscapedName(false), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.checkerEscapedName(false), ForbiddenException.class,
                withPerms(new StringPermission("org:acme:service")));
    }

    @Test
    void checkerMatchesRawEscapedValueInAction() {
        assertSuccess(() -> bean.checkerEscapedAction(true), "checkerEscapedAction", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.checkerEscapedAction(false), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    void checkerMatchesRawEscapedValueInBothNameAndAction() {
        assertSuccess(() -> bean.checkerEscapedBoth(true), "checkerEscapedBoth", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.checkerEscapedBoth(false), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    void unescapedColonCheckerMatchesWithoutParsing() {
        assertSuccess(() -> bean.unescapedColonChecker(true), "unescapedColonChecker", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.unescapedColonChecker(false), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    void escapedColonWithoutCheckerGrantedByIdentityPermission() {
        assertSuccess(() -> bean.escapedColonNoChecker(), "escapedColonNoChecker",
                withPerms(new StringPermission("ns:perm")));
        assertFailureFor(() -> bean.escapedColonNoChecker(), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.escapedColonNoChecker(), ForbiddenException.class,
                withPerms(new StringPermission("ns", "perm")));
    }

    @Test
    void checkerAndIdentityPermissionInOneOfRelation() {
        assertSuccess(() -> bean.checkerOrIdentityPerm(true), "checkerOrIdentityPerm", USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.checkerOrIdentityPerm(false), "checkerOrIdentityPerm",
                withPerms(new StringPermission("scope:admin:read")));
        assertFailureFor(() -> bean.checkerOrIdentityPerm(false), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.checkerOrIdentityPerm(false), ForbiddenException.class,
                withPerms(new StringPermission("scope", "read")));
    }

    @Test
    void repeatedPermissionsWithCheckers() {
        assertSuccess(() -> bean.repeatedCheckerEscaped(true, true), "repeatedCheckerEscaped", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.repeatedCheckerEscaped(true, false), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.repeatedCheckerEscaped(false, true), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    private static AuthData withPerms(Permission... perms) {
        return new AuthData(Set.of("user"), false, "user", Set.of(perms), true);
    }

    @ApplicationScoped
    public static class SecuredBean {

        @PermissionsAllowed("org" + EC + "acme" + EC + "service")
        String checkerEscapedName(boolean allow) {
            return "checkerEscapedName";
        }

        @PermissionsAllowed("scope:act" + EC + "ion")
        String checkerEscapedAction(boolean allow) {
            return "checkerEscapedAction";
        }

        @PermissionsAllowed("ns" + EC + "scope:act" + EC + "ion")
        String checkerEscapedBoth(boolean allow) {
            return "checkerEscapedBoth";
        }

        @PermissionsAllowed("read:write")
        String unescapedColonChecker(boolean allow) {
            return "unescapedColonChecker";
        }

        @PermissionsAllowed("ns" + EC + "perm")
        String escapedColonNoChecker() {
            return "escapedColonNoChecker";
        }

        @PermissionsAllowed({ "scope:read", "scope" + EC + "admin" + EC + "read" })
        String checkerOrIdentityPerm(boolean scopeRead) {
            return "checkerOrIdentityPerm";
        }

        @PermissionsAllowed("perm" + EC + "a")
        @PermissionsAllowed("perm" + EC + "b")
        String repeatedCheckerEscaped(boolean a, boolean b) {
            return "repeatedCheckerEscaped";
        }
    }

    @ApplicationScoped
    public static class PermissionCheckers {

        @PermissionChecker("org" + EC + "acme" + EC + "service")
        boolean canAccessOrgAcmeService(boolean allow) {
            return allow;
        }

        @PermissionChecker("scope:act" + EC + "ion")
        boolean canScopeAction(boolean allow) {
            return allow;
        }

        @PermissionChecker("ns" + EC + "scope:act" + EC + "ion")
        boolean canNsScopeAction(boolean allow) {
            return allow;
        }

        @PermissionChecker("read:write")
        boolean canReadWrite(boolean allow) {
            return allow;
        }

        @PermissionChecker("scope:read")
        boolean canScopeRead(boolean scopeRead) {
            return scopeRead;
        }

        @PermissionChecker("perm" + EC + "a")
        boolean canPermA(boolean a) {
            return a;
        }

        @PermissionChecker("perm" + EC + "b")
        boolean canPermB(boolean b) {
            return b;
        }
    }
}
