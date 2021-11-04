package io.quarkus.security.test.cdi;

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
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnFinalMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithSecuredFinalMethod.class, IdentityMock.class,
                            AuthData.class, SecurityTestUtils.class));

    @Inject
    BeanWithSecuredFinalMethod bean;

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        assertFailureFor(() -> bean.securedMethod(), UnauthorizedException.class, ANONYMOUS);
        assertSuccess(() -> bean.securedMethod(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldFailToAccessCompletely() {
        assertFailureFor(() -> bean.otherSecuredMethod("whatever"), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.otherSecuredMethod("whatever"), ForbiddenException.class, USER, ADMIN);
    }

}
