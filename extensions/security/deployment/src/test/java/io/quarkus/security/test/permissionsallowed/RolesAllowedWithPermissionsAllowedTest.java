package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Collections;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
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

public class RolesAllowedWithPermissionsAllowedTest {

    private static final String SUCCESS = "success";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean securedBean;

    @Test
    public void testMethodLevelAnnotationHasPriority() {
        // test user with permission and without role can access method
        var permission = new AuthData(null, false, "permission", Set.of(new StringPermission("permission")));
        assertSuccess(() -> securedBean.securedBean(), SUCCESS, permission);

        // test user with role and without permission can't access method
        var role = new AuthData(Collections.singleton("role"), false, "role");
        assertFailureFor(() -> securedBean.securedBean(), ForbiddenException.class, role);

        // test user with role and with different permission can't access method
        var roleAndDifferentPermission = new AuthData(Collections.singleton("role"), false, "role",
                Set.of(new StringPermission("different")));
        assertFailureFor(() -> securedBean.securedBean(), ForbiddenException.class, roleAndDifferentPermission);

        // test user with role and with same permission can access method
        var roleAndPermission = new AuthData(Collections.singleton("role"), false, "role",
                Set.of(new StringPermission("permission")));
        assertSuccess(() -> securedBean.securedBean(), SUCCESS, roleAndPermission);
    }

    @RolesAllowed("role")
    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed(value = "permission")
        public String securedBean() {
            return SUCCESS;
        }

    }
}
