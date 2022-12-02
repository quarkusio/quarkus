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
import io.quarkus.spring.security.deployment.springapp.ComponentWithClassAnnotation;
import io.quarkus.spring.security.deployment.springapp.DenyAllOnClass;
import io.quarkus.spring.security.deployment.springapp.Person;
import io.quarkus.spring.security.deployment.springapp.Roles;
import io.quarkus.test.QuarkusUnitTest;

public class SpringPreAuthorizeClassAnnotatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Roles.class,
                            Person.class,
                            ComponentWithClassAnnotation.class,
                            DenyAllOnClass.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Inject
    private ComponentWithClassAnnotation springComponent;

    @Inject
    private DenyAllOnClass denyAllOnClass;

    @Test
    public void testUnannotated() {
        assertFailureFor(() -> springComponent.unannotated(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.unannotated(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.unannotated(), "unannotated", USER);
    }

    @Test
    public void testRestrictedOnMethod() {
        assertFailureFor(() -> springComponent.restrictedOnMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.restrictedOnMethod(), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.restrictedOnMethod(), "restrictedOnMethod", ADMIN);
    }

    @Test
    public void testSecuredWithSecuredAnnotation() {
        assertFailureFor(() -> springComponent.securedWithSecuredAnnotation(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.securedWithSecuredAnnotation(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.securedWithSecuredAnnotation(), "securedWithSecuredAnnotation", USER);
    }

    @Test
    public void testNoAnnotation() {
        assertFailureFor(() -> denyAllOnClass.noAnnotation(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> denyAllOnClass.noAnnotation(), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> denyAllOnClass.noAnnotation(), ForbiddenException.class, USER);
    }

    @Test
    public void testPermitAll() {
        assertSuccess(() -> denyAllOnClass.permitAll(), "permitAll", ANONYMOUS);
        assertSuccess(() -> denyAllOnClass.permitAll(), "permitAll", ADMIN);
        assertSuccess(() -> denyAllOnClass.permitAll(), "permitAll", USER);
    }

    @Test
    public void testAnnotatedWithSecured() {
        assertFailureFor(() -> denyAllOnClass.annotatedWithSecured(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> denyAllOnClass.annotatedWithSecured(), ForbiddenException.class, USER);
        assertSuccess(() -> denyAllOnClass.annotatedWithSecured(), "annotatedWithSecured", ADMIN);
    }
}
