package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

public class PermissionCheckerNameAndIdentityPermissionActionsTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    public void testPermissionCheckerForNameOnly() {
        // permission checker is defined for 'read' but not for the 'read:all'
        assertSuccess(() -> bean.readAndReadAll(), "readAndReadAll", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.readAndReadAll(), ForbiddenException.class, USER_WITH_AUGMENTORS);
        // permission checker is defined for 'write' but not for the 'write:all' or 'write:essay'
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(), "writeAndWriteAllAndEssay", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.writeAndWriteAllAndEssay(), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testIdentityPermissionWithActionGrantAccess() {
        // @PermissionsAllowed({ "read", "read:all" }) says one of: either 'read' is granted by permission checker
        // or 'read:all' is granted by identity permissions
        var userWithReadAll = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read", "all")), true);
        assertSuccess(() -> bean.readAndReadAll(), "readAndReadAll", userWithReadAll);
        var userWithReadNothing = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read", "nothing")),
                true);
        assertFailureFor(() -> bean.readAndReadAll(), ForbiddenException.class, userWithReadNothing);
        // check 'read' can only be granted by the checker method
        var userWithRead = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read")), true);
        assertFailureFor(() -> bean.readAndReadAll(), ForbiddenException.class, userWithRead);
    }

    @Test
    public void testIdentityPermissionWithMultipleActionsGrantAccess() {
        // @PermissionsAllowed({ "write:all", "write", "write:essay" }) says one of: either 'write' is granted
        // by permission checker or 'write:all' or 'write:essay' is granted by identity permissions
        var userWithWriteAll = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "all")), true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(), "writeAndWriteAllAndEssay", userWithWriteAll);
        var userWithWriteEssay = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "essay")),
                true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(), "writeAndWriteAllAndEssay", userWithWriteEssay);
        var userWithWriteEssayAndAll = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("write", "essay", "all")), true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(), "writeAndWriteAllAndEssay", userWithWriteEssayAndAll);
        var userWithWriteNothing = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "nothing")),
                true);
        assertFailureFor(() -> bean.writeAndWriteAllAndEssay(), ForbiddenException.class, userWithWriteNothing);
        // check 'write' can only be granted by the checker method
        var userWithWrite = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write")), true);
        assertFailureFor(() -> bean.writeAndWriteAllAndEssay(), ForbiddenException.class, userWithWrite);
    }

    @Test
    public void testInclusivePermsGrantedByCheckerAndActionsByIdentityPermission() {
        // @PermissionsAllowed(value = { "execute", "execute:all", "execute:dir" }, inclusive = true)
        // check both "execute" granted by the checker method and "execute:all", "execute:dir"

        // access is denied because permission checker requires admin role
        var userWithExecuteDirAndAll = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("execute", "all", "dir")), true);
        assertFailureFor(() -> bean.executeAndAllAndDir(), ForbiddenException.class, userWithExecuteDirAndAll);
        var userWithExecuteNothing = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("execute", "nothing")),
                true);
        assertFailureFor(() -> bean.executeAndAllAndDir(), ForbiddenException.class, userWithExecuteNothing);

        // access is granted as permission checker sees 'admin' role and identity has required permission with actions
        var adminWithExecuteDirAndAll = new AuthData(Set.of("admin"), false, "admin",
                Set.of(new StringPermission("execute", "all", "dir")), true);
        assertSuccess(() -> bean.executeAndAllAndDir(), "executeAndAllAndDir", adminWithExecuteDirAndAll);
        var adminWithExecuteNothing = new AuthData(Set.of("admin"), false, "admin",
                Set.of(new StringPermission("execute", "nothing")), true);
        assertFailureFor(() -> bean.executeAndAllAndDir(), ForbiddenException.class, adminWithExecuteNothing);
    }

    @ApplicationScoped
    public static class SecuredBean {

        @PermissionsAllowed({ "read", "read:all" })
        String readAndReadAll() {
            return "readAndReadAll";
        }

        @PermissionsAllowed({ "write:all", "write", "write:essay" })
        String writeAndWriteAllAndEssay() {
            return "writeAndWriteAllAndEssay";
        }

        @PermissionsAllowed(value = { "execute", "execute:all", "execute:dir" }, inclusive = true)
        String executeAndAllAndDir() {
            return "executeAndAllAndDir";
        }
    }

    @ApplicationScoped
    public static class PermissionCheckers {

        @PermissionChecker("read")
        boolean canRead(SecurityIdentity identity) {
            return identity.hasRole("admin");
        }

        @PermissionChecker("write")
        boolean canWrite(SecurityIdentity identity) {
            return identity.hasRole("admin");
        }

        @PermissionChecker("execute")
        boolean canExecute(SecurityIdentity identity) {
            return identity.hasRole("admin");
        }
    }
}
