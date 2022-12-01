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
import io.quarkus.spring.security.deployment.springapp.Person;
import io.quarkus.spring.security.deployment.springapp.Roles;
import io.quarkus.spring.security.deployment.springapp.SpringComponent;
import io.quarkus.spring.security.deployment.springapp.SpringService;
import io.quarkus.test.QuarkusUnitTest;

public class SpringPreAuthorizeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Roles.class,
                            Person.class,
                            SpringComponent.class,
                            SpringService.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Inject
    private SpringComponent springComponent;

    @Inject
    private SpringService springService;

    @Test
    public void testAccessibleForAdminOnly() {
        assertFailureFor(() -> springComponent.accessibleForAdminOnly(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.accessibleForAdminOnly(), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.accessibleForAdminOnly(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void testAccessibleForUserOnly() {
        assertFailureFor(() -> springComponent.accessibleForUserOnly(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.accessibleForUserOnly(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.accessibleForUserOnly(), "accessibleForUserOnly", USER);
    }

    @Test
    public void testAccessibleForUserOnlyString() {
        assertFailureFor(() -> springComponent.accessibleForUserOnlyString(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.accessibleForUserOnlyString(), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.accessibleForUserOnlyString(), "accessibleForUserOnlyString", USER);
    }

    @Test
    public void testAccessibleForUserAndAdmin() {
        assertFailureFor(() -> springService.accessibleForUserAndAdmin(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> springService.accessibleForUserAndAdmin(), "accessibleForUserAndAdmin", USER);
        assertSuccess(() -> springService.accessibleForUserAndAdmin(), "accessibleForUserAndAdmin", ADMIN);
    }

    @Test
    public void testAccessibleForUserAndAdminMixedTypes() {
        assertFailureFor(() -> springService.accessibleForUserAndAdminMixedTypes(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> springService.accessibleForUserAndAdminMixedTypes(), "accessibleForUserAndAdminMixedTypes", USER);
        assertSuccess(() -> springService.accessibleForUserAndAdminMixedTypes(), "accessibleForUserAndAdminMixedTypes", ADMIN);
    }

    @Test
    public void testAccessibleByAuthenticatedUsers() {
        assertFailureFor(() -> springService.accessibleByAuthenticatedUsers(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> springService.accessibleByAuthenticatedUsers(), "authenticated", USER);
        assertSuccess(() -> springService.accessibleByAuthenticatedUsers(), "authenticated", ADMIN);
    }

    @Test
    public void testAccessibleByAnonymous() {
        assertSuccess(() -> springService.accessibleByAnonymousUser(), "anonymous", ANONYMOUS);
        assertFailureFor(() -> springService.accessibleByAnonymousUser(), ForbiddenException.class, USER);
        assertFailureFor(() -> springService.accessibleByAnonymousUser(), ForbiddenException.class, ADMIN);
    }

    @Test
    public void testPrincipalNameIs() {
        assertFailureFor(() -> springComponent.principalNameIs(null, "whatever", null), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.principalNameIs(null, "whatever", null), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.principalNameIs(null, "user", null), "user", USER);
        assertFailureFor(() -> springComponent.principalNameIs(null, "whatever", null), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.principalNameIs(null, "admin", null), "admin", ADMIN);
    }

    @Test
    public void testPrincipalNameIsNot() {
        assertFailureFor(() -> springComponent.principalNameIsNot("whatever"), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.principalNameIsNot("user"), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.principalNameIsNot("whatever"), "whatever", USER);
        assertFailureFor(() -> springComponent.principalNameIsNot("admin"), ForbiddenException.class, ADMIN);
        assertSuccess(() -> springComponent.principalNameIsNot("whatever"), "whatever", ADMIN);
    }

    @Test
    public void testPrincipalNameFromObject() {
        assertFailureFor(() -> springComponent.principalNameFromObject(new Person("whatever")), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(() -> springComponent.principalNameFromObject(new Person("whatever")), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.principalNameFromObject(new Person("user")), "user", USER);
    }

    @Test
    public void testNotSecured() {
        assertSuccess(() -> springComponent.notSecured(), "notSecured", ANONYMOUS);
        assertSuccess(() -> springComponent.notSecured(), "notSecured", USER);
        assertSuccess(() -> springComponent.notSecured(), "notSecured", ADMIN);
    }

    @Test
    public void testSecuredWithSecuredAnnotation() {
        assertFailureFor(() -> springComponent.securedWithSecuredAnnotation(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> springComponent.securedWithSecuredAnnotation(), ForbiddenException.class, USER);
        assertSuccess(() -> springComponent.securedWithSecuredAnnotation(), "securedWithSecuredAnnotation", ADMIN);
    }
}
