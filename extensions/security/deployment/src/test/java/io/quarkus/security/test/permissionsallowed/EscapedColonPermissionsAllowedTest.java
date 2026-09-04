package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

class EscapedColonPermissionsAllowedTest {

    private static final String EC = "\\:";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    void escapedColonsInNameNoAction() {
        assertSuccess(() -> bean.escapedColonNameOnly(), "escapedColonNameOnly",
                withPerms(new StringPermission("system:role:query1")));
        assertFailureFor(() -> bean.escapedColonNameOnly(), ForbiddenException.class,
                withPerms(new StringPermission("system")));
        assertFailureFor(() -> bean.escapedColonNameOnly(), ForbiddenException.class,
                withPerms(new StringPermission("system", "role:query1")));
    }

    @Test
    void escapedColonInNameWithAction() {
        assertSuccess(() -> bean.escapedColonNameWithAction(), "escapedColonNameWithAction",
                withPerms(new StringPermission("system:role", "query")));
        assertFailureFor(() -> bean.escapedColonNameWithAction(), ForbiddenException.class,
                withPerms(new StringPermission("system:role")));
        assertFailureFor(() -> bean.escapedColonNameWithAction(), ForbiddenException.class,
                withPerms(new StringPermission("system", "role")));
    }

    @Test
    void escapedColonInAction() {
        assertSuccess(() -> bean.escapedColonInAction(), "escapedColonInAction",
                withPerms(new StringPermission("perm", "role:query")));
        assertFailureFor(() -> bean.escapedColonInAction(), ForbiddenException.class,
                withPerms(new StringPermission("perm", "role")));
    }

    @Test
    void mixedEscapedAndPlainValues() {
        assertSuccess(() -> bean.mixedValues(), "mixedValues", withPerms(new StringPermission("system:role:query1")));
        assertSuccess(() -> bean.mixedValues(), "mixedValues", withPerms(new StringPermission("simple")));
        assertSuccess(() -> bean.mixedValues(), "mixedValues", withPerms(new StringPermission("name", "action")));
        assertFailureFor(() -> bean.mixedValues(), ForbiddenException.class, withPerms(new StringPermission("system")));
    }

    @Test
    void inclusiveWithEscapedColons() {
        assertSuccess(() -> bean.inclusiveEscaped(), "inclusiveEscaped",
                withPerms(new StringPermission("system:role", "read"), new StringPermission("system:role")));
        assertSuccess(() -> bean.inclusiveEscaped(), "inclusiveEscaped",
                withPerms(new StringPermission("system:role", "read")));
        assertFailureFor(() -> bean.inclusiveEscaped(), ForbiddenException.class,
                withPerms(new StringPermission("system:role")));
    }

    @Test
    void repeatedPermissionsWithEscapedColons() {
        assertSuccess(() -> bean.repeatedEscaped(), "repeatedEscaped",
                withPerms(new StringPermission("ns:read"), new StringPermission("ns:write")));
        assertFailureFor(() -> bean.repeatedEscaped(), ForbiddenException.class, withPerms(new StringPermission("ns:read")));
        assertFailureFor(() -> bean.repeatedEscaped(), ForbiddenException.class, withPerms(new StringPermission("ns:write")));
    }

    private static AuthData withPerms(Permission... perms) {
        return new AuthData(Set.of("user"), false, "user", Set.of(perms), true);
    }

    @ApplicationScoped
    public static class SecuredBean {

        @PermissionsAllowed("system" + EC + "role" + EC + "query1")
        String escapedColonNameOnly() {
            return "escapedColonNameOnly";
        }

        @PermissionsAllowed("system" + EC + "role:query")
        String escapedColonNameWithAction() {
            return "escapedColonNameWithAction";
        }

        @PermissionsAllowed("perm:role" + EC + "query")
        String escapedColonInAction() {
            return "escapedColonInAction";
        }

        @PermissionsAllowed({ "system" + EC + "role" + EC + "query1", "simple", "name:action" })
        String mixedValues() {
            return "mixedValues";
        }

        @PermissionsAllowed(value = { "system" + EC + "role", "system" + EC + "role:read" }, inclusive = true)
        String inclusiveEscaped() {
            return "inclusiveEscaped";
        }

        @PermissionsAllowed("ns" + EC + "read")
        @PermissionsAllowed("ns" + EC + "write")
        String repeatedEscaped() {
            return "repeatedEscaped";
        }
    }
}
