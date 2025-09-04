package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.annotation.Priority;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CustomAuthorizationControllerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class,
                            CustomAuthorizationController.class, SecuredBean.class));

    @Inject
    CustomAuthorizationController authorizationController;

    @Inject
    SecuredBean bean;

    @Test
    public void testAuthorizationEnabled() {
        authorizationController.enabled = true;
        assertFailureFor(() -> bean.methodLevelRolesAllowed(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.methodLevelRolesAllowed(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.classLevelPermitAll(), "classLevelPermitAll", USER);
        assertSuccess(() -> bean.methodLevelRolesAllowed(), "methodLevelRolesAllowed", ADMIN);
    }

    @Test
    public void testAuthorizationDisabled() {
        authorizationController.enabled = false;
        assertSuccess(() -> bean.methodLevelRolesAllowed(), "methodLevelRolesAllowed", ANONYMOUS);
        assertSuccess(() -> bean.methodLevelRolesAllowed(), "methodLevelRolesAllowed", USER);
        assertSuccess(() -> bean.classLevelPermitAll(), "classLevelPermitAll", USER);
    }

    @Unremovable
    @Alternative
    @Priority(Interceptor.Priority.LIBRARY_AFTER)
    @Singleton
    public static final class CustomAuthorizationController extends AuthorizationController {

        private volatile boolean enabled = true;

        public boolean isAuthorizationEnabled() {
            return enabled;
        }
    }

    @PermitAll
    @ApplicationScoped
    public static class SecuredBean {

        public String classLevelPermitAll() {
            return "classLevelPermitAll";
        }

        @RolesAllowed("admin")
        public String methodLevelRolesAllowed() {
            return "methodLevelRolesAllowed";
        }

    }
}
