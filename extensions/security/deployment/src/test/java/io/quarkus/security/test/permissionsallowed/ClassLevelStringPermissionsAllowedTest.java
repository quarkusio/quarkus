package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ClassLevelStringPermissionsAllowedTest {

    static final String WRITE_PERMISSION = "write";
    static final String WRITE_PERMISSION_BEAN = "write:bean";
    static final String READ_PERMISSION = "read";
    static final String READ_PERMISSION_BEAN = "read:bean";

    private final AuthData USER = new AuthData(Collections.singleton("user"), false, "user",
            Set.of(createPermission("read", (String[]) null)));
    private final AuthData ADMIN = new AuthData(Collections.singleton("admin"), false, "admin",
            Set.of(createPermission("write", (String[]) null)));

    // mechanism for class level annotations does not differ from method level (where we do extensive testing),
    // therefore what we really do want to test is annotation detection and smoke test
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SingleAnnotationWriteBean writeBean;

    @Inject
    SingleAnnotationWriteWithActionBean writeWithActionBean;

    @Inject
    MultipleWriteReadBean writeReadBean;

    @Inject
    MultipleWriteReadWithActionBean writeReadWithActionBean;

    protected Permission createPermission(String name, String... actions) {
        return new StringPermission(name, actions);
    }

    @Test
    public void testSinglePermission() {
        // identity has one permission, annotation has same permission
        assertSuccess(() -> writeBean.write(), WRITE_PERMISSION, ADMIN);
        assertSuccess(writeBean.writeNonBlocking(), WRITE_PERMISSION, ADMIN);

        // identity has one permission, annotation has different permission
        assertFailureFor(() -> writeBean.write(), ForbiddenException.class, USER);
        assertFailureFor(writeBean.writeNonBlocking(), ForbiddenException.class, USER);
    }

    @Test
    public void testSinglePermissionWithAction() {
        // identity has one permission and action, annotation has same permission and action
        final var admin = new AuthData(Collections.singleton("admin"), false, "admin",
                permission(WRITE_PERMISSION, "bean"));
        assertSuccess(() -> writeWithActionBean.write(), WRITE_PERMISSION, admin);
        assertSuccess(writeWithActionBean.writeNonBlocking(), WRITE_PERMISSION, admin);

        // identity has one permission and action, annotation has same permission and different action
        final var admin2 = new AuthData(Collections.singleton("admin"), false, "admin",
                permission(WRITE_PERMISSION, "bean2"));
        assertFailureFor(() -> writeWithActionBean.write(), ForbiddenException.class, admin2);
        assertFailureFor(writeWithActionBean.writeNonBlocking(), ForbiddenException.class, admin2);
    }

    @Test
    public void testMultiplePermissions() {
        // identity has one permission, annotation has 2 permissions, one of them is matching
        assertSuccess(() -> writeReadBean.write(), WRITE_PERMISSION, ADMIN);
        assertSuccess(writeReadBean.writeNonBlocking(), WRITE_PERMISSION, ADMIN);
        assertSuccess(() -> writeReadBean.read(), READ_PERMISSION, USER);
        assertSuccess(writeReadBean.readNonBlocking(), READ_PERMISSION, USER);

        // identity has 2 permissions, annotation has different permission
        final var user2 = new AuthData(Collections.singleton("user2"), false, "user2",
                permission(READ_PERMISSION + 2, "bean2"));
        assertFailureFor(() -> writeReadBean.write(), ForbiddenException.class, user2);
        assertFailureFor(writeReadBean.writeNonBlocking(), ForbiddenException.class, user2);
    }

    @Test
    public void testMultiplePermissionsWithActions() {
        final var admin = new AuthData(Collections.singleton("admin"), false, "admin",
                permission(WRITE_PERMISSION, "bean"));
        final var user = new AuthData(Collections.singleton("user"), false, "user",
                permission(READ_PERMISSION, "bean"));

        // identity has one permission and action, annotation has 2 permissions and action, one of permission/action is
        // matching
        assertSuccess(() -> writeReadWithActionBean.write(), WRITE_PERMISSION_BEAN, admin);
        assertSuccess(writeReadWithActionBean.writeNonBlocking(), WRITE_PERMISSION_BEAN, admin);
        assertSuccess(() -> writeReadWithActionBean.read(), READ_PERMISSION_BEAN, user);
        assertSuccess(writeReadWithActionBean.readNonBlocking(), READ_PERMISSION_BEAN, user);

        // identity has one permission and action, annotation has 2 permissions and action, one permission is matching,
        // but action differs
        final var admin2 = new AuthData(Collections.singleton("admin"), false, "admin",
                permission(WRITE_PERMISSION, "bean2"));
        assertFailureFor(() -> writeReadWithActionBean.write(), ForbiddenException.class, admin2);
        assertFailureFor(writeReadWithActionBean.writeNonBlocking(), ForbiddenException.class, admin2);
    }

    static Set<Permission> permission(String permissionName, String... actions) {
        return Set.of(new StringPermission(permissionName, actions));
    }

    @PermissionsAllowed(WRITE_PERMISSION)
    @Singleton
    public static class SingleAnnotationWriteBean {

        public final String write() {
            return WRITE_PERMISSION;
        }

        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }
    }

    @PermissionsAllowed(WRITE_PERMISSION_BEAN)
    @Singleton
    public static class SingleAnnotationWriteWithActionBean {

        public final String write() {
            return WRITE_PERMISSION;
        }

        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }
    }

    @PermissionsAllowed({ WRITE_PERMISSION, READ_PERMISSION })
    @Singleton
    public static class MultipleWriteReadBean {

        public final String write() {
            return WRITE_PERMISSION;
        }

        public final String read() {
            return READ_PERMISSION;
        }

        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }

        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION);
        }
    }

    @PermissionsAllowed({ WRITE_PERMISSION_BEAN, READ_PERMISSION_BEAN })
    @Singleton
    public static class MultipleWriteReadWithActionBean {

        public final String write() {
            return WRITE_PERMISSION_BEAN;
        }

        public final String read() {
            return READ_PERMISSION_BEAN;
        }

        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION_BEAN);
        }

        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION_BEAN);
        }
    }
}
