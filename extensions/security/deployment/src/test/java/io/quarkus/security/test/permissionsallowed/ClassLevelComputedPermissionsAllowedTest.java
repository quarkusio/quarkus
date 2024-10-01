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
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ClassLevelComputedPermissionsAllowedTest {

    private static final String IGNORED = "ignored";
    private static final Set<Permission> CHECKING_PERMISSION = Set.of(new Permission("permission_name") {
        @Override
        public boolean implies(Permission permission) {
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
    AutodetectParamsBean autodetectParamsBean;

    @Inject
    ExplicitlyMatchedParamsBean explicitlyMatchedParamsBean;

    @Test
    public void testAutodetectedParams() {
        var anonymous = new AuthData(null, true, null, CHECKING_PERMISSION);
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // secured class methods have exactly same parameters as Permission constructor (except of permission name and actions)
        assertSuccess(() -> autodetectParamsBean.autodetect("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(() -> autodetectParamsBean.autodetect("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(() -> autodetectParamsBean.autodetect("what", "ever", "?"), UnauthorizedException.class, anonymous);
        assertSuccess(autodetectParamsBean.autodetectNonBlocking("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(autodetectParamsBean.autodetectNonBlocking("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(autodetectParamsBean.autodetectNonBlocking("what", "ever", "?"), UnauthorizedException.class,
                anonymous);
    }

    @Test
    public void testExplicitlyMatchedParams() {
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // secured class methods have multiple params and Permission constructor selects one of them
        assertSuccess(() -> explicitlyMatchedParamsBean.autodetect("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(() -> explicitlyMatchedParamsBean.autodetect("what", "ever", "?"), ForbiddenException.class, user);
        assertSuccess(explicitlyMatchedParamsBean.autodetectNonBlocking("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(explicitlyMatchedParamsBean.autodetectNonBlocking("what", "ever", "?"), ForbiddenException.class,
                user);

        // differs from above in params number, which means number of different methods can be secured via class-level annotation
        assertSuccess(() -> explicitlyMatchedParamsBean.autodetect("world"), SUCCESS, user);
        assertFailureFor(() -> explicitlyMatchedParamsBean.autodetect("ever"), ForbiddenException.class, user);
        assertSuccess(explicitlyMatchedParamsBean.autodetectNonBlocking("world"), SUCCESS, user);
        assertFailureFor(explicitlyMatchedParamsBean.autodetectNonBlocking("ever"), ForbiddenException.class, user);
    }

    @PermissionsAllowed(value = IGNORED, permission = AllStrAutodetectedPermission.class)
    @Singleton
    public static class AutodetectParamsBean {

        public String autodetect(String hello, String world, String exclamationMark) {
            return SUCCESS;
        }

        public Uni<String> autodetectNonBlocking(String hello, String world, String exclamationMark) {
            return Uni.createFrom().item(SUCCESS);
        }

    }

    @PermissionsAllowed(value = IGNORED, permission = AllStrExplicitlyMatchedPermission.class, params = "world")
    @Singleton
    public static class ExplicitlyMatchedParamsBean {

        public String autodetect(String hello, String world, String exclamationMark) {
            return SUCCESS;
        }

        public Uni<String> autodetectNonBlocking(String hello, String world, String exclamationMark) {
            return Uni.createFrom().item(SUCCESS);
        }

        public String autodetect(String world) {
            return SUCCESS;
        }

        public Uni<String> autodetectNonBlocking(String world) {
            return Uni.createFrom().item(SUCCESS);
        }

    }

    public static class AllStrAutodetectedPermission extends Permission {
        private final boolean pass;

        public AllStrAutodetectedPermission(String name, String[] actions, String exclamationMark, String world, String hello) {
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

    public static class AllStrExplicitlyMatchedPermission extends Permission {
        private final boolean pass;

        public AllStrExplicitlyMatchedPermission(String name, String world) {
            super(name);
            this.pass = "world".equals(world);
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
            AllStrExplicitlyMatchedPermission that = (AllStrExplicitlyMatchedPermission) o;
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
