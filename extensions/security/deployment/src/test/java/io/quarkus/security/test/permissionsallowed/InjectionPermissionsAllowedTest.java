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

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class InjectionPermissionsAllowedTest {

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
    SecuredBean securedBean;

    @Test
    public void testInjection() {
        var anonymous = new AuthData(null, true, null, CHECKING_PERMISSION);
        var user = new AuthData(Collections.singleton("user"), false, "user", CHECKING_PERMISSION);

        // tests you can access bean via 'Arc.container()'
        assertSuccess(() -> securedBean.injection("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(() -> securedBean.injection("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(() -> securedBean.injection("what", "ever", "?"), UnauthorizedException.class, anonymous);
        assertSuccess(securedBean.injectionNonBlocking("hello", "world", "!"), SUCCESS, user);
        assertFailureFor(securedBean.injectionNonBlocking("what", "ever", "?"), ForbiddenException.class, user);
        assertFailureFor(securedBean.injectionNonBlocking("what", "ever", "?"), UnauthorizedException.class,
                anonymous);
    }

    @PermissionsAllowed(value = IGNORED, permission = AllStrAutodetectedPermission.class)
    @Singleton
    public static class SecuredBean {

        public String injection(String hello, String world, String exclamationMark) {
            return SUCCESS;
        }

        public Uni<String> injectionNonBlocking(String hello, String world, String exclamationMark) {
            return Uni.createFrom().item(SUCCESS);
        }

    }

    public static class AllStrAutodetectedPermission extends Permission {
        private final boolean pass;

        public AllStrAutodetectedPermission(String name, String[] actions, String hello, String world, String exclamationMark) {
            super(name);
            var sourceOfTruth = Arc.container().instance(SourceOfTruth.class).get();
            this.pass = "hello".equals(hello) && "world".equals(world) && "!".equals(exclamationMark)
                    && sourceOfTruth.shouldPass();
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

    @Unremovable
    @Singleton
    public static class SourceOfTruth {

        public boolean shouldPass() {
            return true;
        }

    }

}
