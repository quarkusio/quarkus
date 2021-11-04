package io.quarkus.spring.security.deployment;

import static io.quarkus.security.test.utils.IdentityMock.*;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.spring.security.deployment.app.BeanWithSpringSecurityAnnotations;
import io.quarkus.spring.security.deployment.app.BeanWithSpringSecurityMethodAnnotations;
import io.quarkus.spring.security.deployment.app.SpringSecuredSubClass;
import io.quarkus.test.QuarkusUnitTest;

public class SpringSecuredAnnotatedTest {

    @Inject
    @Named(BeanWithSpringSecurityMethodAnnotations.NAME)
    BeanWithSpringSecurityMethodAnnotations beanWithSpringSecurityMethodAnnotations;

    @Inject
    BeanWithSpringSecurityAnnotations beanWithSpringSecurityAnnotations;

    @Inject
    SpringSecuredSubClass springSecuredSubClass;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithSpringSecurityMethodAnnotations.class,
                            BeanWithSpringSecurityAnnotations.class,
                            SpringSecuredSubClass.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Test
    public void shouldRunUnannotated() {
        assertSuccess(() -> beanWithSpringSecurityMethodAnnotations.unannotated(), "unannotated", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldRestrictOnMethod() {
        assertFailureFor(() -> beanWithSpringSecurityAnnotations.restrictedOnMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanWithSpringSecurityAnnotations.restrictedOnMethod(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> beanWithSpringSecurityAnnotations.restrictedOnMethod(), "accessibleForUserOnly", USER);
    }

    @Test
    public void shouldRestrictToUserOnMethod() {
        assertFailureFor(() -> beanWithSpringSecurityMethodAnnotations.restricted(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanWithSpringSecurityMethodAnnotations.restricted(), ForbiddenException.class, USER);
        assertSuccess(() -> beanWithSpringSecurityMethodAnnotations.restricted(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldRestrictOnClass() {
        assertSuccess(() -> beanWithSpringSecurityAnnotations.restricted(), "accessibleForAdminOnly", ADMIN);
        assertFailureFor(() -> beanWithSpringSecurityAnnotations.restricted(), ForbiddenException.class, USER);
    }

    @Test
    public void shouldRestrictOnSubClass() {
        assertFailureFor(() -> springSecuredSubClass.restricted(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springSecuredSubClass.restricted(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springSecuredSubClass.restricted(), "restrictedOnMethod", USER);
    }

    @Test
    public void shouldAllowOnSubClass() {
        assertSuccess(() -> springSecuredSubClass.unannotated(), "unannotated", ADMIN, ANONYMOUS, USER);
        assertSuccess(() -> beanWithSpringSecurityAnnotations.unannotated(), "unannotated", ADMIN, ANONYMOUS, USER);
    }

}
