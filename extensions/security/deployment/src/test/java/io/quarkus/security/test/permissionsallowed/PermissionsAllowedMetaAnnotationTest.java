package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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

public class PermissionsAllowedMetaAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class,
                            SingleNoArgsPermissionsAllowedMetaAnnotation.class, StringPermissionsAllowedMetaAnnotation.class,
                            StringDeniedPermissionsAllowedMetaAnnotation.class));

    @Inject
    PermissionsAllowedMethodLevelWithoutActions withoutActionsMethodLevelBean;

    @Inject
    PermissionsAllowedClassLevelWithoutActions withoutActionsClassLevelBean;

    @Inject
    StringPermissionsAllowedClassLevel stringPermissionsAllowedClassLevelBean;

    @Inject
    StringPermissionsAllowedMethodLevel stringPermissionsAllowedMethodLevelBean;

    @Inject
    MetaAnnotationOnMethodLevelOverridesClassLevel methodLevelMetaOverridesClassLevelBean;

    @Inject
    MetaAnnotationOnClassLevelOverriddenByMethodLevel classLevelMetaOverriddenByMethodLevelBean;

    @Inject
    RepeatedAnnotation repeatedAnnotation;

    @Test
    public void testCustomAnnotationWithoutActions_MethodLevel() {
        testWriterPermission(() -> withoutActionsMethodLevelBean.write(), CustomPermissionNameOnly::new);
    }

    @Test
    public void testCustomAnnotationWithoutActions_ClassLevel() {
        testWriterPermission(() -> withoutActionsClassLevelBean.write(), CustomPermissionNameOnly::new);
    }

    @Test
    public void testStringPermission_MethodLevel() {
        testWriterPermission(() -> stringPermissionsAllowedMethodLevelBean.write(), StringPermission::new);
    }

    @Test
    public void testStringPermission_ClassLevel() {
        testWriterPermission(() -> stringPermissionsAllowedClassLevelBean.write(), StringPermission::new);
    }

    @Test
    public void testMetaAnnotationOnMethodLevelOverridesClassLevel() {
        testWriterPermission(() -> methodLevelMetaOverridesClassLevelBean.write(), StringPermission::new);
        testDenyAllPermission(() -> methodLevelMetaOverridesClassLevelBean.other());
    }

    @Test
    public void testClassLevelMetaOverriddenByMethodLevelBean() {
        testWriterPermission(() -> classLevelMetaOverriddenByMethodLevelBean.write(), StringPermission::new);
        testDenyAllPermission(() -> classLevelMetaOverriddenByMethodLevelBean.other());
    }

    @Test
    public void testRepeatedAnnotation() {
        testWriterPermission(() -> repeatedAnnotation.write(), StringPermission::new);

        // 'other requires string permissions 'deny-all' and 'repeated'
        AuthData writer = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new StringPermission("write")));

        AuthData denyAll = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new StringPermission("deny-all")));

        assertFailureFor(() -> repeatedAnnotation.other(), ForbiddenException.class, denyAll);
        assertFailureFor(() -> repeatedAnnotation.other(), ForbiddenException.class, writer);

        AuthData denyAllRepeated = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new StringPermission("deny-all"), new StringPermission("repeated")));
        assertSuccess(() -> repeatedAnnotation.other(), "other", denyAllRepeated);
    }

    private void testDenyAllPermission(Supplier<String> supplier) {
        AuthData writer = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new StringPermission("write")));

        AuthData denyAll = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new StringPermission("deny-all")));

        assertSuccess(supplier::get, "other", denyAll);
        assertFailureFor(supplier::get, ForbiddenException.class, writer);
    }

    private static void testWriterPermission(Supplier<String> supplier, Function<String, Permission> permissionCreator) {
        AuthData writer = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(permissionCreator.apply("write")));

        assertSuccess(supplier, "write", writer);

        AuthData reader = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(permissionCreator.apply("reader")));

        assertFailureFor(supplier::get, ForbiddenException.class, reader);
    }

    /**
     * This permission does not accept actions, it is important in order to test class instantiation that differs
     * for actions/without actions.
     */
    public static class CustomPermissionNameOnly extends Permission {

        private final Permission delegate;

        public CustomPermissionNameOnly(String name) {
            super(name);
            this.delegate = new StringPermission(name);
        }

        @Override
        public boolean implies(Permission permission) {
            if (permission instanceof CustomPermissionNameOnly) {
                return delegate.implies(((CustomPermissionNameOnly) permission).delegate);
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CustomPermissionNameOnly that = (CustomPermissionNameOnly) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        @Override
        public String getActions() {
            return delegate.getActions();
        }
    }

    @Singleton
    public static class PermissionsAllowedMethodLevelWithoutActions {

        @SingleNoArgsPermissionsAllowedMetaAnnotation
        public final String write() {
            return "write";
        }

    }

    @SingleNoArgsPermissionsAllowedMetaAnnotation
    @Singleton
    public static class PermissionsAllowedClassLevelWithoutActions {

        public final String write() {
            return "write";
        }

    }

    @StringPermissionsAllowedMetaAnnotation
    @Singleton
    public static class StringPermissionsAllowedClassLevel {

        public final String write() {
            return "write";
        }

    }

    @Singleton
    public static class StringPermissionsAllowedMethodLevel {

        @StringPermissionsAllowedMetaAnnotation
        public final String write() {
            return "write";
        }

    }

    @PermissionsAllowed("deny-all")
    @Singleton
    public static class MetaAnnotationOnMethodLevelOverridesClassLevel {

        @StringPermissionsAllowedMetaAnnotation
        public final String write() {
            return "write";
        }

        public final String other() {
            return "other";
        }
    }

    @StringDeniedPermissionsAllowedMetaAnnotation
    @Singleton
    public static class MetaAnnotationOnClassLevelOverriddenByMethodLevel {

        @PermissionsAllowed("write")
        public final String write() {
            return "write";
        }

        public final String other() {
            return "other";
        }

    }

    @StringDeniedPermissionsAllowedMetaAnnotation
    @PermissionsAllowed("repeated")
    @Singleton
    public static class RepeatedAnnotation {

        @StringPermissionsAllowedMetaAnnotation
        public final String write() {
            return "write";
        }

        public final String other() {
            return "other";
        }

    }

}
