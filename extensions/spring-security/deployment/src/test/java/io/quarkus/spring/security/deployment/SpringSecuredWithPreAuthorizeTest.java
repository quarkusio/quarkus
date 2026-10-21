package io.quarkus.spring.security.deployment;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A method annotated with both {@link Secured} and {@link PreAuthorize} must satisfy both annotations.
 */
public class SpringSecuredWithPreAuthorizeTest {

    private static final AuthData ADMIN_AND_USER = new AuthData(Set.of("admin", "user"), false, "adminAndUser");

    @Inject
    SecuredAndPreAuthorizedBean bean;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredAndPreAuthorizedBean.class, IdentityMock.class, AuthData.class,
                            SecurityTestUtils.class));

    @Test
    public void shouldRequireBothAnnotationsToPass() {
        assertFailureFor(() -> bean.securedAndPreAuthorized(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.securedAndPreAuthorized(), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.securedAndPreAuthorized(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.securedAndPreAuthorized(), "securedAndPreAuthorized", ADMIN_AND_USER);
    }

    @ApplicationScoped
    public static class SecuredAndPreAuthorizedBean {

        @Secured("admin")
        @PreAuthorize("hasRole('user')")
        public String securedAndPreAuthorized() {
            return "securedAndPreAuthorized";
        }
    }
}
