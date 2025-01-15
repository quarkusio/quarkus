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
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionCheckerNameWithColonsTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    public void testIdentityPermissionWithActionGrantAccess() {
        // @PermissionsAllowed({ "read", "read:all" }) says one of: either 'read' is granted by permission checker
        // or 'read:all' is granted by identity permissions
        var userWithReadAll = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read", "all")), true);
        assertSuccess(() -> bean.readAndReadAll(false), "readAndReadAll", userWithReadAll);
        var userWithReadNothing = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read", "nothing")),
                true);
        assertFailureFor(() -> bean.readAndReadAll(false), ForbiddenException.class, userWithReadNothing);
        // check 'read' is granted by the checker method
        assertSuccess(() -> bean.readAndReadAll(true), "readAndReadAll", userWithReadNothing);
        // check 'read' can only be granted by the checker method
        var userWithRead = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("read")), true);
        assertFailureFor(() -> bean.readAndReadAll(false), ForbiddenException.class, userWithRead);
    }

    @Test
    public void testIdentityPermissionWithMultipleActionsGrantsAccess() {
        // @PermissionsAllowed({ "write:all", "write", "write:essay" }) says one of: either 'write' is granted
        // by permission checker or 'write:all' or 'write:essay' is granted by identity permissions
        var userWithWriteAll = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "all")), true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(false), "writeAndWriteAllAndEssay", userWithWriteAll);
        var userWithWriteEssay = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "essay")),
                true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(false), "writeAndWriteAllAndEssay", userWithWriteEssay);
        var userWithWriteEssayAndAll = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("write", "essay", "all")), true);
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(false), "writeAndWriteAllAndEssay", userWithWriteEssayAndAll);
        var userWithWriteNothing = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write", "nothing")),
                true);
        assertFailureFor(() -> bean.writeAndWriteAllAndEssay(false), ForbiddenException.class, userWithWriteNothing);
        // check 'write' is granted by the checker method
        assertSuccess(() -> bean.writeAndWriteAllAndEssay(true), "writeAndWriteAllAndEssay", userWithWriteNothing);
        // check 'write' can only be granted by the checker method
        var userWithWrite = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("write")), true);
        assertFailureFor(() -> bean.writeAndWriteAllAndEssay(false), ForbiddenException.class, userWithWrite);
    }

    @Test
    public void testInclusivePermsAreOnlyGrantedByChecker() {
        // @PermissionsAllowed(value = { "execute", "execute:all", "execute:dir" }, inclusive = true)
        // check all "execute", "execute:all", "execute:dir" are granted by the checker,
        // and they cannot be granted by the identity permissions at all

        // access is denied because permission checkers require 'true'
        var userWithExecuteDirAndAll = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("execute", "all", "dir")), true);
        assertFailureFor(() -> bean.executeAndAllAndDir(true, true, false), ForbiddenException.class, userWithExecuteDirAndAll);
        assertFailureFor(() -> bean.executeAndAllAndDir(true, false, true), ForbiddenException.class, userWithExecuteDirAndAll);
        assertFailureFor(() -> bean.executeAndAllAndDir(false, true, true), ForbiddenException.class, userWithExecuteDirAndAll);

        // access is granted because all arguments are true
        assertSuccess(() -> bean.executeAndAllAndDir(true, true, true), "executeAndAllAndDir", userWithExecuteDirAndAll);

        // access is denied as anonymous user is not allow access any resources annotated with the @PermissionsAllowed
        // because anonymous users can't have permissions
        assertFailureFor(() -> bean.executeAndAllAndDir(true, true, true), UnauthorizedException.class,
                new AuthData(IdentityMock.ANONYMOUS, true));
    }

    @Test
    public void testInclusivePermsAreOnlyGrantedByCheckerAndExtraPermission() {
        // @PermissionsAllowed(value = { "delete", "delete:all", "delete:dir", "purge" }, inclusive = true)
        // check all "delete", "delete:all", "delete:dir" are granted by the checker,
        // and they cannot be granted by the identity permissions at all
        // but for the 'purge', identity permission can be used and the checker doesn't need to exist

        // access is denied because permission checkers require 'true' even though user has all the identity permissions
        var userWithDeleteDirAndAllAndPurge = new AuthData(Set.of("user"), false, "user",
                Set.of(new StringPermission("delete", "all", "dir"), new StringPermission("purge")), true);
        assertFailureFor(() -> bean.deleteAndAllAndDir(true, true, false), ForbiddenException.class,
                userWithDeleteDirAndAllAndPurge);
        assertFailureFor(() -> bean.deleteAndAllAndDir(true, false, true), ForbiddenException.class,
                userWithDeleteDirAndAllAndPurge);
        assertFailureFor(() -> bean.deleteAndAllAndDir(false, true, true), ForbiddenException.class,
                userWithDeleteDirAndAllAndPurge);
        var userWithPurge = new AuthData(Set.of("user"), false, "user", Set.of(new StringPermission("purge")), true);
        assertFailureFor(() -> bean.deleteAndAllAndDir(true, true, false), ForbiddenException.class, userWithPurge);
        assertFailureFor(() -> bean.deleteAndAllAndDir(true, false, true), ForbiddenException.class, userWithPurge);
        assertFailureFor(() -> bean.deleteAndAllAndDir(false, true, true), ForbiddenException.class, userWithPurge);

        // access is granted because all arguments are true
        assertSuccess(() -> bean.deleteAndAllAndDir(true, true, true), "deleteAndAllAndDir", userWithDeleteDirAndAllAndPurge);
        assertSuccess(() -> bean.deleteAndAllAndDir(true, true, true), "deleteAndAllAndDir", userWithPurge);

        // access is not granted because identity doesn't have 'purge' permission
        assertFailureFor(() -> bean.deleteAndAllAndDir(true, true, true), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testPermissionCheckersForNamesWithActionSeparatorsOnly() {
        // @PermissionsAllowed({ "edit:all", "edit", "edit:essay" })
        // permission checker is defined for: edit:all, edit:essay

        // assert that edit:all and edit:essay permission checkers grant access
        assertSuccess(() -> bean.editAndEditAllAndEssay(true, false), "editAndEditAllAndEssay", USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.editAndEditAllAndEssay(false, true), "editAndEditAllAndEssay", USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.editAndEditAllAndEssay(true, true), "editAndEditAllAndEssay", USER_WITH_AUGMENTORS);

        // assert that 'edit' can be granted by identity permission
        var userWithEdit = new AuthData(USER, true, new StringPermission("edit"));
        assertSuccess(() -> bean.editAndEditAllAndEssay(false, false), "editAndEditAllAndEssay", userWithEdit);

        // assert user without either the identity permission or granted access by the checker method cannot access
        assertFailureFor(() -> bean.editAndEditAllAndEssay(false, false), ForbiddenException.class, USER_WITH_AUGMENTORS);

        // @PermissionsAllowed({ "list:files", "list:dir" })
        // permission checker is defined for 'list:files'

        // is allowed because the checker grants access
        assertSuccess(() -> bean.listFilesAndDir(true), "listFilesAndDir", USER_WITH_AUGMENTORS);

        // is not allowed because the checker does not grant access
        assertFailureFor(() -> bean.listFilesAndDir(false), ForbiddenException.class, USER_WITH_AUGMENTORS);

        // is not allowed because identity permission cannot grant access with the 'list:files' when such checker exists
        var userListFiles = new AuthData(USER, true, new StringPermission("list", "files"));
        assertFailureFor(() -> bean.listFilesAndDir(false), ForbiddenException.class, userListFiles);

        // is allowed because identity permission can grant access with the 'list:dir' as no such checker exists
        var userListDir = new AuthData(USER, true, new StringPermission("list", "dir"));
        assertSuccess(() -> bean.listFilesAndDir(false), "listFilesAndDir", userListDir);

        // @PermissionsAllowed({ "list:files", "list:links" })
        // there is a permission checker for both permissions
        assertSuccess(() -> bean.listFilesAndLinks(true, true), "listFilesAndLinks", USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.listFilesAndLinks(true, false), "listFilesAndLinks", USER_WITH_AUGMENTORS);
        assertSuccess(() -> bean.listFilesAndLinks(false, true), "listFilesAndLinks", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.listFilesAndLinks(false, false), ForbiddenException.class, USER_WITH_AUGMENTORS);
        var userWithListFilesAndLinks = new AuthData(USER, true, new StringPermission("list", "files", "links"));
        assertFailureFor(() -> bean.listFilesAndLinks(false, false), ForbiddenException.class, userWithListFilesAndLinks);
    }

    @Test
    public void testInclusivePermissionCheckersAndRepeatedAnnotations() {
        // @PermissionsAllowed("cut:array")
        // @PermissionsAllowed(value = { "cut:blob", "cut:chars" }, inclusive = true)
        // @PermissionsAllowed(value = { "cut:text", "cut:binary" }, inclusive = true)
        // @PermissionsAllowed({ "cut:text", "cut:binary" }) - SHOULD be irrelevant
        assertSuccess(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(true, true, true, true, true),
                "cutTextAndBinaryAndBlobAndArrayAndChars", USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(false, true, true, true, true),
                ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(true, false, true, true, true),
                ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(true, true, false, true, true),
                ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(true, true, true, false, true),
                ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(true, true, true, true, false),
                ForbiddenException.class, USER_WITH_AUGMENTORS);
        var userWithAllCutActions = new AuthData(USER, true,
                new StringPermission("cut", "array", "blob", "chars", "text", "binary"));
        // assert failures as only checkers can grant these permissions
        assertFailureFor(() -> bean.cutTextAndBinaryAndBlobAndArrayAndChars(false, false, false, false, false),
                ForbiddenException.class, userWithAllCutActions);
    }

    @ApplicationScoped
    public static class SecuredBean {

        @PermissionsAllowed({ "read", "read:all" })
        String readAndReadAll(boolean read) {
            return "readAndReadAll";
        }

        @PermissionsAllowed({ "write:all", "write", "write:essay" })
        String writeAndWriteAllAndEssay(boolean write) {
            return "writeAndWriteAllAndEssay";
        }

        @PermissionsAllowed(value = { "execute", "execute:all", "execute:dir" }, inclusive = true)
        String executeAndAllAndDir(boolean execute, boolean executeAll, boolean executeDir) {
            return "executeAndAllAndDir";
        }

        @PermissionsAllowed(value = { "delete", "delete:all", "delete:dir", "purge" }, inclusive = true)
        String deleteAndAllAndDir(boolean delete, boolean deleteAll, boolean deleteDir) {
            return "deleteAndAllAndDir";
        }

        @PermissionsAllowed({ "edit:all", "edit", "edit:essay" })
        String editAndEditAllAndEssay(boolean editAll, boolean editEssay) {
            return "editAndEditAllAndEssay";
        }

        @PermissionsAllowed({ "list:files", "list:dir" })
        String listFilesAndDir(boolean listFiles) {
            return "listFilesAndDir";
        }

        @PermissionsAllowed({ "list:files", "list:links" })
        String listFilesAndLinks(boolean listFiles, boolean listLinks) {
            return "listFilesAndLinks";
        }

        @PermissionsAllowed("cut:array")
        @PermissionsAllowed(value = { "cut:blob", "cut:chars" }, inclusive = true)
        @PermissionsAllowed(value = { "cut:text", "cut:binary" }, inclusive = true)
        @PermissionsAllowed({ "cut:text", "cut:binary" }) // this one SHOULD not have effect due to the instance above
        String cutTextAndBinaryAndBlobAndArrayAndChars(boolean cutText, boolean cutBinary, boolean cutArray, boolean cutChars,
                boolean cutBlob) {
            return "cutTextAndBinaryAndBlobAndArrayAndChars";
        }
    }

    @ApplicationScoped
    public static class PermissionCheckers {

        @PermissionChecker("read")
        boolean canRead(boolean read) {
            return read;
        }

        @PermissionChecker("write")
        boolean canWrite(boolean write) {
            return write;
        }

        @PermissionChecker("execute")
        boolean canExecute(boolean execute) {
            return execute;
        }

        @PermissionChecker("execute:all")
        boolean canExecuteAll(boolean executeAll) {
            return executeAll;
        }

        @PermissionChecker("execute:dir")
        boolean canExecuteDir(boolean executeDir) {
            return executeDir;
        }

        @PermissionChecker("delete")
        boolean canDelete(boolean delete) {
            return delete;
        }

        @PermissionChecker("delete:all")
        boolean canDeleteAll(boolean deleteAll) {
            return deleteAll;
        }

        @PermissionChecker("delete:dir")
        boolean canDeleteDir(boolean deleteDir) {
            return deleteDir;
        }

        @PermissionChecker("edit:all")
        boolean canEditAll(boolean editAll) {
            return editAll;
        }

        @PermissionChecker("edit:essay")
        boolean canEditEssay(boolean editEssay) {
            return editEssay;
        }

        @PermissionChecker("list:files")
        boolean canListFiles(boolean listFiles) {
            return listFiles;
        }

        @PermissionChecker("list:links")
        boolean canListLinks(boolean listLinks) {
            return listLinks;
        }

        @PermissionChecker("cut:text")
        boolean canCutText(boolean cutText) {
            return cutText;
        }

        @PermissionChecker("cut:binary")
        boolean canCutBinary(boolean cutBinary) {
            return cutBinary;
        }

        @PermissionChecker("cut:blob")
        boolean canCutBlob(boolean cutBlob) {
            return cutBlob;
        }

        @PermissionChecker("cut:array")
        boolean canCutArray(boolean cutArray) {
            return cutArray;
        }

        @PermissionChecker("cut:chars")
        boolean canCutChars(boolean cutChars) {
            return cutChars;
        }

    }
}
