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
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MethodLevelComputedPermissionsAllowedTest {

    private static final String IGNORED = "ignored";
    private static final Set<Permission> CHECKING_PERMISSION = Set.of(new Permission("permission_name") {
        @Override
        public boolean implies(Permission permission) {
            // the point here is to invoke permission check
            // as tests in this class decides based on method inputs
            // if incoming permission is StringPermission, we pass it identical permission
            // as we need to test combination of computed and non-computed checks
            if (permission instanceof StringPermission) {
                return permission.implies(new StringPermission(permission.getName()));
            }
            return permission.implies(this);
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String getActions() {
            return null;
        }
    });
    private static final String SUCCESS = "success";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean securedBean;

    @Test
    public void testAutodetectedParams() {
        var anonymous = new AuthData(null, true, null, CHECKING_PERMISSION);
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // secured method has exactly same parameters as Permission constructor (except of permission name and actions)
        assertSuccess(() -> securedBean.autodetect("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(() -> securedBean.autodetect("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(() -> securedBean.autodetect("what", "ever", "?"), UnauthorizedException.class, anonymous);
        assertSuccess(securedBean.autodetectNonBlocking("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(securedBean.autodetectNonBlocking("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(securedBean.autodetectNonBlocking("what", "ever", "?"), UnauthorizedException.class, anonymous);

        // secured method has more parameters with all variety of data types, while Permission constructor accepts 3 'int' params
        assertSuccess(() -> securedBean.autodetect(1, "something", 2, 3, new Object(), null), SUCCESS, user);
        assertFailureFor(() -> securedBean.autodetect(1, "something", 5, 3, new Object(), null), ForbiddenException.class,
                user);
        assertSuccess(securedBean.autodetectNonBlocking(1, "something", 2, 3, new Object(), null), SUCCESS, user);
        assertFailureFor(securedBean.autodetectNonBlocking(1, "something", 5, 3, new Object(), null), ForbiddenException.class,
                user);

        // inheritance (constructor is called with actions and that is checked)
        assertSuccess(() -> securedBean.autodetect(new Child(true)), SUCCESS, user);
        assertFailureFor(() -> securedBean.autodetect(new Child(false)), ForbiddenException.class, user);
        assertSuccess(securedBean.autodetectNonBlocking(new Child(true)), SUCCESS, user);
        assertFailureFor(securedBean.autodetectNonBlocking(new Child(false)), ForbiddenException.class, user);

        // in addition to inheritance calls right above, here 2 permissions are created and tested for 1 annotation (both with actions)
        assertSuccess(() -> securedBean.autodetectMultiplePermissions(new Child(true)), SUCCESS, user);
        assertFailureFor(() -> securedBean.autodetectMultiplePermissions(new Child(false)), ForbiddenException.class, user);
        assertSuccess(securedBean.autodetectMultiplePermissionsNonBlocking(new Child(true)), SUCCESS, user);
        assertFailureFor(securedBean.autodetectMultiplePermissionsNonBlocking(new Child(false)), ForbiddenException.class,
                user);
    }

    @Test
    public void testExplicitlyMarkedParams() {
        var anonymous = new AuthData(null, true, null, CHECKING_PERMISSION);
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // secured method 'sayHelloWorld' accepts multiple arguments, however only 'hello', 'world' and 'exclamationMark'
        // are passed to Permission constructor as specified by 'params' attribute
        assertSuccess(() -> securedBean.explicitlyDeclaredParams("something", "hello", "whatever", "world", "!", 1), SUCCESS,
                user);
        assertFailureFor(() -> securedBean.explicitlyDeclaredParams("something", "test", "whatever", "world", "!", 1),
                ForbiddenException.class, user);
        assertFailureFor(() -> securedBean.explicitlyDeclaredParams("something", "hello", "whatever", "rest", "!", 1),
                UnauthorizedException.class, anonymous);

        // same as above, however method returns reactive data type, therefore the check is done asynchronously
        assertSuccess(securedBean.explicitlyDeclaredParamsNonBlocking("something", "hello", "whatever", "world", "!", 1),
                SUCCESS, user);
        assertFailureFor(securedBean.explicitlyDeclaredParamsNonBlocking("something", "test", "whatever", "world", "!", 1),
                ForbiddenException.class, user);
        assertFailureFor(securedBean.explicitlyDeclaredParamsNonBlocking("something", "hello", "whatever", "rest", "!", 1),
                UnauthorizedException.class, anonymous);

        // inheritance - Permission constructor accepts Parent while secured method accepts Child, should work
        // as user explicitly marked param via 'params = "obj"'
        // this test also differs from above ones in that Permission does not accept actions
        assertSuccess(
                securedBean.explicitlyDeclaredParamsInheritanceNonBlocking("something", "hello", "whatever", "world", "!", 1,
                        new Child(true)),
                SUCCESS, user);
        assertFailureFor(
                securedBean.explicitlyDeclaredParamsInheritanceNonBlocking("something", "test", "whatever", "world", "!", 1,
                        new Child(false)),
                ForbiddenException.class, user);
        assertSuccess(
                () -> securedBean.explicitlyDeclaredParamsInheritance("something", "hello", "whatever", "world", "!", 1,
                        new Child(true)),
                SUCCESS, user);
        assertFailureFor(
                () -> securedBean.explicitlyDeclaredParamsInheritance("something", "test", "whatever", "world", "!", 1,
                        new Child(false)),
                ForbiddenException.class, user);
    }

    @Test
    public void testCombinationOfComputedAndPlainPermissions() {
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // one permission is computed (AKA its accepts method params) and the other one is created at runtime init
        assertSuccess(() -> securedBean.combination(new Child(true)), SUCCESS, user);
        assertFailureFor(() -> securedBean.combination(new Child(false)), ForbiddenException.class, user);
        assertSuccess(securedBean.combinationNonBlocking(new Child(true)), SUCCESS, user);
        assertFailureFor(securedBean.combinationNonBlocking(new Child(false)), ForbiddenException.class, user);
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed(value = IGNORED, permission = AllStrAutodetectedPermission.class)
        public String autodetect(String hello, String world, String exclamationMark) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = IGNORED, permission = AllIntAutodetectedPermission.class)
        public String autodetect(int one, String world, int two, int three, Object obj1, Object obj2) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = "permissionName:action1234", permission = InheritanceWithActionsPermission.class)
        public String autodetect(Parent obj) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = { "permissionName:action1234",
                "permission1:action1" }, permission = InheritanceWithActionsPermission.class)
        public String autodetectMultiplePermissions(Parent obj) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = "permissionName:action1234", permission = InheritanceWithActionsPermission.class)
        public Uni<String> autodetectNonBlocking(Parent obj) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed(value = { "permissionName:action1234",
                "permission1:action1" }, permission = InheritanceWithActionsPermission.class)
        public Uni<String> autodetectMultiplePermissionsNonBlocking(Parent obj) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed(value = IGNORED, permission = AllStrAutodetectedPermission.class)
        public Uni<String> autodetectNonBlocking(String hello, String world, String exclamationMark) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed(value = IGNORED, permission = AllIntAutodetectedPermission.class)
        public Uni<String> autodetectNonBlocking(int one, String world, int two, int three, Object obj1, Object obj2) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed(value = IGNORED, permission = AllStrMatchingParamsPermission.class, params = {
                "hello", "world", "exclamationMark" })
        public String explicitlyDeclaredParams(String something, String hello, String whatever, String world,
                String exclamationMark, int i) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = IGNORED, permission = AllStrMatchingParamsPermission.class, params = {
                "hello", "world", "exclamationMark" })
        public Uni<String> explicitlyDeclaredParamsNonBlocking(String something, String hello, String whatever, String world,
                String exclamationMark, int i) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed(value = IGNORED, permission = InheritancePermission.class, params = "obj")
        public String explicitlyDeclaredParamsInheritance(String something, String hello, String whatever, String world,
                String exclamationMark, int i, Child obj) {
            return SUCCESS;
        }

        @PermissionsAllowed(value = IGNORED, permission = InheritancePermission.class, params = "obj")
        public Uni<String> explicitlyDeclaredParamsInheritanceNonBlocking(String something, String hello, String whatever,
                String world, String exclamationMark, int i, Child obj) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed("read")
        @PermissionsAllowed(value = "permissionName:action1234", permission = InheritanceWithActionsPermission.class)
        public Uni<String> combinationNonBlocking(Parent obj) {
            return Uni.createFrom().item(SUCCESS);
        }

        @PermissionsAllowed("read")
        @PermissionsAllowed(value = "permissionName:action1234", permission = InheritanceWithActionsPermission.class)
        public String combination(Parent obj) {
            return SUCCESS;
        }
    }

    public interface Parent {

        boolean shouldPass();

    }

    public static class Child implements Parent {

        private final boolean pass;

        public Child(boolean pass) {
            this.pass = pass;
        }

        @Override
        public boolean shouldPass() {
            return pass;
        }
    }

    public static class InheritanceWithActionsPermission extends Permission {
        private final boolean pass;

        public InheritanceWithActionsPermission(String name, String[] actions, Parent obj) {
            super(name);
            this.pass = obj != null && obj.shouldPass() && actions != null && actions.length == 1
                    && "action1234".equals(actions[0]);
        }

        @Override
        public boolean implies(Permission permission) {
            return pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            InheritanceWithActionsPermission that = (InheritanceWithActionsPermission) o;
            return pass == that.pass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pass);
        }

        @Override
        public String getActions() {
            return null;
        }

    }

    public static class InheritancePermission extends Permission {
        private final boolean pass;

        public InheritancePermission(String name, Parent obj) {
            super(name);
            this.pass = obj != null && obj.shouldPass();
        }

        @Override
        public boolean implies(Permission permission) {
            return pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            InheritancePermission that = (InheritancePermission) o;
            return pass == that.pass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pass);
        }

        @Override
        public String getActions() {
            return null;
        }

    }

    public static class AllStrAutodetectedPermission extends Permission {
        private final boolean pass;

        public AllStrAutodetectedPermission(String name, String[] actions, String hello, String exclamationMark, String world) {
            super(name);
            this.pass = "hello".equals(hello) && "world".equals(world) && "!".equals(exclamationMark);
        }

        @Override
        public boolean implies(Permission permission) {
            return pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AllStrAutodetectedPermission that = (AllStrAutodetectedPermission) o;
            return pass == that.pass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pass);
        }

        @Override
        public String getActions() {
            return null;
        }

    }

    public static class AllIntAutodetectedPermission extends Permission {
        private final boolean pass;

        public AllIntAutodetectedPermission(String name, String[] actions, int three, int two, int one) {
            super(name);
            // here we expect to match 'int' secured method parameters and have them passed here
            // considering secured method has also 'Object' and 'String' parameters, the task is more complex
            this.pass = one == 1 && two == 2 && three == 3;
        }

        @Override
        public boolean implies(Permission permission) {
            return pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AllStrAutodetectedPermission that = (AllStrAutodetectedPermission) o;
            return pass == that.pass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pass);
        }

        @Override
        public String getActions() {
            return null;
        }

    }

    public static class AllStrMatchingParamsPermission extends Permission {
        private final boolean pass;

        public AllStrMatchingParamsPermission(String name, String[] actions, String hello, String world,
                String exclamationMark) {
            super(name);
            // constructor param names must exactly match secured method params as we explicitly marked them
            this.pass = "hello".equals(hello) && "world".equals(world) && "!".equals(exclamationMark);
        }

        @Override
        public boolean implies(Permission permission) {
            return pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AllStrMatchingParamsPermission that = (AllStrMatchingParamsPermission) o;
            return pass == that.pass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pass);
        }

        @Override
        public String getActions() {
            return null;
        }

    }
}
