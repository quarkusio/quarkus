package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Collections;
import java.util.Objects;
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

public class MethodLevelCustomPermissionsAllowedTest extends AbstractMethodLevelPermissionsAllowedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    PermissionsAllowedNameOnlyBean nameOnlyBean;

    @Inject
    PermissionsAllowedNameAndActionsOnlyBean nameAndActionsBean;

    @Inject
    MultiplePermissionsAllowedBean multiplePermissionsAllowedBean;

    @Inject
    PermissionsAllowedWithoutActions withoutActionsBean;

    @Override
    protected MultiplePermissionsAllowedBeanI getMultiplePermissionsAllowedBean() {
        return multiplePermissionsAllowedBean;
    }

    @Override
    protected PermissionsAllowedNameOnlyBeanI getPermissionsAllowedNameOnlyBean() {
        return nameOnlyBean;
    }

    @Override
    protected PermissionsAllowedNameAndActionsOnlyBeanI getPermissionsAllowedNameAndActionsOnlyBean() {
        return nameAndActionsBean;
    }

    @Override
    protected Permission createPermission(String name, String... actions) {
        return new CustomPermission(name, actions);
    }

    @Test
    public void testCustomAnnotationWithoutActions() {
        AuthData admin = new AuthData(Collections.singleton("admin"), false, "admin",
                Set.of(new CustomPermissionNameOnly("write")));

        // custom annotation with constructor that does not accept actions
        assertSuccess(() -> withoutActionsBean.write(), WRITE_PERMISSION, admin);
        assertSuccess(withoutActionsBean.writeNonBlocking(), WRITE_PERMISSION, admin);

        // identity has one permission, annotation has different permission
        assertFailureFor(() -> withoutActionsBean.prohibited(), ForbiddenException.class, admin);
        assertFailureFor(withoutActionsBean.prohibitedNonBlocking(), ForbiddenException.class, admin);
    }

    /**
     * This permission does not accept actions, it is important in order to test class instantiation that differs for
     * actions/without actions.
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

    public static class CustomPermission extends Permission {

        private final Permission delegate;

        public CustomPermission(String name, String... actions) {
            super(name);
            this.delegate = new StringPermission(name, actions);
        }

        @Override
        public boolean implies(Permission permission) {
            if (permission instanceof CustomPermission) {
                return delegate.implies(((CustomPermission) permission).delegate);
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CustomPermission that = (CustomPermission) o;
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
    public static class PermissionsAllowedWithoutActions {

        @PermissionsAllowed(value = WRITE_PERMISSION, permission = CustomPermissionNameOnly.class)
        public final String write() {
            return WRITE_PERMISSION;
        }

        @PermissionsAllowed(value = WRITE_PERMISSION, permission = CustomPermissionNameOnly.class)
        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }

        @PermissionsAllowed(value = "prohibited", permission = CustomPermissionNameOnly.class)
        public final void prohibited() {
        }

        @PermissionsAllowed(value = "prohibited", permission = CustomPermissionNameOnly.class)
        public final Uni<Void> prohibitedNonBlocking() {
            return Uni.createFrom().nullItem();
        }
    }

    @Singleton
    public static class PermissionsAllowedNameOnlyBean implements PermissionsAllowedNameOnlyBeanI {

        @PermissionsAllowed(value = WRITE_PERMISSION, permission = CustomPermission.class)
        public final String write() {
            return WRITE_PERMISSION;
        }

        @PermissionsAllowed(value = READ_PERMISSION, permission = CustomPermission.class)
        public final String read() {
            return READ_PERMISSION;
        }

        @PermissionsAllowed(value = WRITE_PERMISSION, permission = CustomPermission.class)
        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }

        @PermissionsAllowed(value = READ_PERMISSION, permission = CustomPermission.class)
        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION);
        }

        @PermissionsAllowed(value = "prohibited", permission = CustomPermission.class)
        public final void prohibited() {
        }

        @PermissionsAllowed(value = "prohibited", permission = CustomPermission.class)
        public final Uni<Void> prohibitedNonBlocking() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed(value = { "one", "two", "three", READ_PERMISSION }, permission = CustomPermission.class)
        public final String multiple() {
            return MULTIPLE_PERMISSION;
        }

        @PermissionsAllowed(value = { "one", "two", "three", READ_PERMISSION }, permission = CustomPermission.class)
        public final Uni<String> multipleNonBlocking() {
            return Uni.createFrom().item(MULTIPLE_PERMISSION);
        }

    }

    @Singleton
    public static class PermissionsAllowedNameAndActionsOnlyBean implements PermissionsAllowedNameAndActionsOnlyBeanI {

        @PermissionsAllowed(value = WRITE_PERMISSION_BEAN, permission = CustomPermission.class)
        public final String write() {
            return WRITE_PERMISSION_BEAN;
        }

        @PermissionsAllowed(value = READ_PERMISSION_BEAN, permission = CustomPermission.class)
        public final String read() {
            return READ_PERMISSION_BEAN;
        }

        @PermissionsAllowed(value = WRITE_PERMISSION_BEAN, permission = CustomPermission.class)
        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION_BEAN);
        }

        @PermissionsAllowed(value = READ_PERMISSION_BEAN, permission = CustomPermission.class)
        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION_BEAN);
        }

        @PermissionsAllowed(value = "prohibited:bean", permission = CustomPermission.class)
        public final void prohibited() {
        }

        @PermissionsAllowed(value = "prohibited:bean", permission = CustomPermission.class)
        public final Uni<Void> prohibitedNonBlocking() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed(value = { "one:a", "two:b", "three:c",
                READ_PERMISSION_BEAN }, permission = CustomPermission.class)
        public final String multiple() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed(value = { "one:a", "two:b", "three:c",
                READ_PERMISSION_BEAN }, permission = CustomPermission.class)
        public final Uni<String> multipleNonBlocking() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed(value = { "one:a", "two:b", "three:c", "one:b", "two:a", "three:a", READ_PERMISSION_BEAN,
                "read:meal" }, permission = CustomPermission.class)
        public final String multipleActions() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed(value = { "one:a", "two:b", "three:c", "one:b", "two:a", "three:a", READ_PERMISSION_BEAN,
                "read:meal" }, permission = CustomPermission.class)
        public final Uni<String> multipleNonBlockingActions() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three",
                READ_PERMISSION }, permission = CustomPermission.class)
        public final String combination() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three",
                READ_PERMISSION }, permission = CustomPermission.class)
        public final Uni<String> combinationNonBlockingActions() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread",
                "read:meal" }, permission = CustomPermission.class)
        public final String combination2() {
            return "combination2";
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread",
                "read:meal" }, permission = CustomPermission.class)
        public final Uni<String> combination2NonBlockingActions() {
            return Uni.createFrom().item("combination2");
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread",
                "read:meal" }, inclusive = true, permission = CustomPermission.class)
        @Override
        public User inclusive() {
            return new User("Martin");
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread",
                "read:meal" }, inclusive = true, permission = CustomPermission.class)
        @Override
        public Uni<User> inclusiveNonBlocking() {
            return Uni.createFrom().item(new User("Bruno"));
        }

    }

    @Singleton
    public static class MultiplePermissionsAllowedBean implements MultiplePermissionsAllowedBeanI {

        @PermissionsAllowed(value = "update", permission = CustomPermission.class)
        @PermissionsAllowed(value = "create", permission = CustomPermission.class)
        public String createOrUpdate() {
            return "create_or_update";
        }

        @PermissionsAllowed(value = "create", permission = CustomPermission.class)
        @PermissionsAllowed(value = "update", permission = CustomPermission.class)
        public Uni<String> createOrUpdateNonBlocking() {
            return Uni.createFrom().item("create_or_update");
        }

        @PermissionsAllowed(value = "see", permission = CustomPermission.class)
        @PermissionsAllowed(value = "view", permission = CustomPermission.class)
        public String getOne() {
            return "see_or_view_detail";
        }

        @PermissionsAllowed(value = "view", permission = CustomPermission.class)
        @PermissionsAllowed(value = "see", permission = CustomPermission.class)
        public Uni<String> getOneNonBlocking() {
            return Uni.createFrom().item("see_or_view_detail");
        }

        @PermissionsAllowed(value = { "operand1", "operand2", "operand3" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand4", "operand5" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand6", "operand7" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand8" }, permission = CustomPermission.class)
        public Uni<String> predicateNonBlocking() {
            // (operand1 OR operand2 OR operand3) AND (operand4 OR operand5) AND (operand6 OR operand7) AND operand8
            return Uni.createFrom().item(PREDICATE);
        }

        @PermissionsAllowed(value = { "operand1", "operand2", "operand3" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand4", "operand5" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand6", "operand7" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "operand8" }, permission = CustomPermission.class)
        public String predicate() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return PREDICATE;
        }

        @PermissionsAllowed(value = { "permission1:action1",
                "permission2:action2" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "permission1:action2",
                "permission2:action1" }, permission = CustomPermission.class)
        public String actionsPredicate() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return PREDICATE;
        }

        @PermissionsAllowed(value = { "permission1:action1",
                "permission2:action2" }, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "permission1:action2",
                "permission2:action1" }, permission = CustomPermission.class)
        public Uni<String> actionsPredicateNonBlocking() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return Uni.createFrom().item(PREDICATE);
        }

        @PermissionsAllowed(value = { "permission1:action1",
                "permission2:action2" }, inclusive = true, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "permission1:action2",
                "permission2:action1" }, inclusive = true, permission = CustomPermission.class)
        @Override
        public User inclusive() {
            return new User("Sergey");
        }

        @PermissionsAllowed(value = { "permission1:action1",
                "permission2:action2" }, inclusive = true, permission = CustomPermission.class)
        @PermissionsAllowed(value = { "permission1:action2",
                "permission2:action1" }, inclusive = true, permission = CustomPermission.class)
        @Override
        public Uni<User> inclusiveNonBlocking() {
            return Uni.createFrom().item(new User("Stuart"));
        }
    }

}
