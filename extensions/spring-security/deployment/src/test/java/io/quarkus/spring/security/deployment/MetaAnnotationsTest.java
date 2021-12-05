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
import io.quarkus.spring.security.deployment.springapp.BeanWithMetaAnnotations;
import io.quarkus.spring.security.deployment.springapp.IsUser;
import io.quarkus.spring.security.deployment.springapp.IsUserOrAdmin;
import io.quarkus.test.QuarkusUnitTest;

public class MetaAnnotationsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            IsUser.class,
                            IsUserOrAdmin.class,
                            BeanWithMetaAnnotations.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Inject
    BeanWithMetaAnnotations beanWithMetaAnnotations;

    @Test
    public void testPreAuthorizeMetaAnnotationIsUser() {
        assertFailureFor(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUser(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUser(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUser(), "preAuthorizeMetaAnnotationIsUser",
                USER);
    }

    @Test
    public void testPreAuthorizeMetaAnnotationIsUserOrAdmin() {
        assertFailureFor(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUserOrAdmin(), UnauthorizedException.class,
                ANONYMOUS);
        assertSuccess(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUserOrAdmin(),
                "preAuthorizeMetaAnnotationIsUserOrAdmin",
                USER);
        assertSuccess(() -> beanWithMetaAnnotations.preAuthorizeMetaAnnotationIsUserOrAdmin(),
                "preAuthorizeMetaAnnotationIsUserOrAdmin",
                ADMIN);
    }

    @Test
    public void testNotSecured() {
        assertSuccess(() -> beanWithMetaAnnotations.notSecured(), "notSecured", ANONYMOUS);
        assertSuccess(() -> beanWithMetaAnnotations.notSecured(), "notSecured", USER);
        assertSuccess(() -> beanWithMetaAnnotations.notSecured(), "notSecured", ADMIN);
    }

    @Test
    public void testIsSecuredWithSecuredAnnotation() {
        assertFailureFor(() -> beanWithMetaAnnotations.isSecuredWithSecuredAnnotation(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(() -> beanWithMetaAnnotations.isSecuredWithSecuredAnnotation(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> beanWithMetaAnnotations.isSecuredWithSecuredAnnotation(), "isSecuredWithSecuredAnnotation",
                USER);
    }
}
