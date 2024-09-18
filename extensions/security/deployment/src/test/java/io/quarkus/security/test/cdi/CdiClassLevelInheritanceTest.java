package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.app.TestException;
import io.quarkus.security.test.cdi.inheritance.AuthenticatedBean;
import io.quarkus.security.test.cdi.inheritance.DenyAllBean;
import io.quarkus.security.test.cdi.inheritance.PermissionsAllowedBean;
import io.quarkus.security.test.cdi.inheritance.RolesAllowedBean;
import io.quarkus.security.test.cdi.inheritance.SubclassAuthenticatedBean;
import io.quarkus.security.test.cdi.inheritance.SubclassDenyAllBean;
import io.quarkus.security.test.cdi.inheritance.SubclassPermissionsAllowedBean;
import io.quarkus.security.test.cdi.inheritance.SubclassRolesAllowedBean;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CdiClassLevelInheritanceTest {

    @Inject
    SubclassDenyAllBean subclassDenyAll;

    @Inject
    SubclassRolesAllowedBean subclassRolesAllowed;

    @Inject
    SubclassAuthenticatedBean subclassAuthenticated;

    @Inject
    SubclassPermissionsAllowedBean subclassPermissionsAllowed;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class,
                            AuthData.class,
                            DenyAllBean.class,
                            RolesAllowedBean.class,
                            AuthenticatedBean.class,
                            PermissionsAllowedBean.class,
                            SubclassPermissionsAllowedBean.class,
                            SubclassDenyAllBean.class,
                            SubclassRolesAllowedBean.class,
                            SubclassAuthenticatedBean.class,
                            TestException.class,
                            SecurityTestUtils.class));

    @Test
    public void testDenyAllInherited() {
        assertFailureFor(() -> subclassDenyAll.ping(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> subclassDenyAll.ping(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void testRolesAllowedInherited() {
        assertFailureFor(() -> subclassRolesAllowed.ping(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> subclassRolesAllowed.ping(), ForbiddenException.class, USER);
        assertSuccess(() -> subclassRolesAllowed.ping(), RolesAllowedBean.class.getSimpleName(), ADMIN);
    }

    @Test
    public void testAuthenticatedInherited() {
        assertFailureFor(() -> subclassAuthenticated.ping(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> subclassAuthenticated.ping(), AuthenticatedBean.class.getSimpleName(), USER);
        assertSuccess(() -> subclassAuthenticated.ping(), AuthenticatedBean.class.getSimpleName(), ADMIN);
    }

    @Test
    public void testPermissionAllowedInherited() {
        AuthData USER_READ = new AuthData(Collections.singleton("user_read"), false, "user_read",
                Set.of(new StringPermission("read")));
        AuthData USER_WRITE = new AuthData(Collections.singleton("user_write"), false, "user_write",
                Set.of(new StringPermission("write")));
        assertSuccess(() -> subclassPermissionsAllowed.ping(), PermissionsAllowedBean.class.getSimpleName(), USER_READ);
        assertFailureFor(() -> subclassPermissionsAllowed.ping(), ForbiddenException.class, USER_WRITE);
    }
}
