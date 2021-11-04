package io.quarkus.spring.security.deployment;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.spring.security.deployment.springapp.BeanWithAndOrExpressions;
import io.quarkus.test.QuarkusUnitTest;

public class SpringPreAuthorizeWithExpressionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            BeanWithAndOrExpressions.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Inject
    private BeanWithAndOrExpressions bean;

    @Test
    public void testAllowedForUser() {
        assertFailureFor(() -> bean.allowedForUser("user"), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.allowedForUser("user"), ForbiddenException.class, ADMIN);
        assertSuccess(() -> bean.allowedForUser("user"), "allowedForUser", USER);
    }

    @Test
    public void testAllowedForUserOrAdmin() {
        assertFailureFor(() -> bean.allowedForUserOrAdmin(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> bean.allowedForUserOrAdmin(), "allowedForUserOrAdmin", USER);
        assertSuccess(() -> bean.allowedForUserOrAdmin(), "allowedForUserOrAdmin", ADMIN);
    }

    @Test
    public void testAllowedForAdminOrAnonymous() {
        assertFailureFor(() -> bean.allowedForAdminOrAnonymous(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.allowedForAdminOrAnonymous(), "allowedForAdminOrAnonymous", ANONYMOUS);
        assertSuccess(() -> bean.allowedForAdminOrAnonymous(), "allowedForAdminOrAnonymous", ADMIN);
    }
}
