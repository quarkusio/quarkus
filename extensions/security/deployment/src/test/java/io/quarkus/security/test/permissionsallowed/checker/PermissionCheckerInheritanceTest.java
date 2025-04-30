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

import io.quarkus.arc.Unremovable;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionCheckerInheritanceTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    InheritanceSecuredBean securedBean;

    @Test
    public void testCheckerOnAbstractParent() {
        assertSuccess(() -> securedBean.secured_checkerOnParent("parent"), "secured_checkerOnParent", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_checkerOnParent("parent"), ForbiddenException.class, USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_checkerOnParent("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testCheckerOnDefaultInterface() {
        assertSuccess(() -> securedBean.secured_checkerOnInterface("interface"), "secured_checkerOnInterface",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_checkerOnInterface("interface"), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_checkerOnInterface("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testOverloadedMethod() {
        assertSuccess(() -> securedBean.secured_overloadedBaseOne("overloaded_base"), "secured_overloadedBaseOne",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_overloadedBaseOne("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> securedBean.secured_overloadedBaseTwo("overloaded_base_two"), "secured_overloadedBaseTwo",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_overloadedBaseTwo("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> securedBean.secured_overloadedParentOne("overloaded_parent"), "secured_overloadedParentOne",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_overloadedParentOne("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> securedBean.secured_overloadedParentTwo("overloaded_parent_two"), "secured_overloadedParentTwo",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secured_overloadedParentTwo("wrong-value"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class InheritanceSecuredBean {

        @PermissionsAllowed("parent")
        public String secured_checkerOnParent(String param) {
            return "secured_checkerOnParent";
        }

        @PermissionsAllowed("interface")
        public String secured_checkerOnInterface(String param) {
            return "secured_checkerOnInterface";
        }

        @PermissionsAllowed("overloaded_base")
        public String secured_overloadedBaseOne(String param) {
            return "secured_overloadedBaseOne";
        }

        @PermissionsAllowed("overloaded_base_two")
        public String secured_overloadedBaseTwo(String param) {
            return "secured_overloadedBaseTwo";
        }

        @PermissionsAllowed("overloaded_parent")
        public String secured_overloadedParentOne(String param) {
            return "secured_overloadedParentOne";
        }

        @PermissionsAllowed("overloaded_parent_two")
        public String secured_overloadedParentTwo(String param) {
            return "secured_overloadedParentTwo";
        }

    }

    @Unremovable
    @Singleton
    public static class CheckerOnParent_base extends CheckerOnParent_parent {

    }

    @ApplicationScoped
    public static class CheckerMethodOverloaded_base extends CheckerMethodOverloaded_parent {

        @PermissionChecker("overloaded_base")
        boolean overloaded_base(SecurityIdentity identity, String param) {
            return "overloaded_base".equals(param) && identity.hasRole("admin");
        }

        @PermissionChecker("overloaded_base_two")
        boolean overloaded_base(String param, SecurityIdentity identity) {
            return "overloaded_base_two".equals(param) && identity.hasRole("admin");
        }

    }

    public static abstract class CheckerMethodOverloaded_parent {

        @PermissionChecker("overloaded_parent")
        boolean overloaded_parent(String param, SecurityIdentity identity) {
            return "overloaded_parent".equals(param) && identity.hasRole("admin");
        }

        @PermissionChecker("overloaded_parent_two")
        boolean overloaded_parent(SecurityIdentity identity, String param) {
            return "overloaded_parent_two".equals(param) && identity.hasRole("admin");
        }
    }

    public static abstract class CheckerOnParent_parent {

        @Inject
        SecurityIdentity identity;

        @PermissionChecker("parent")
        boolean canAccess(String param) {
            return "parent".equals(param) && identity.hasRole("admin");
        }

    }

    @Unremovable
    @Singleton
    public static class CheckerOnInterface_base implements CheckerOnInterface_interface {

    }

    public interface CheckerOnInterface_interface {

        @PermissionChecker("interface")
        default boolean canAccess(String param, SecurityIdentity identity) {
            return "interface".equals(param) && identity.hasRole("admin");
        }

    }
}
