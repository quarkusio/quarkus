package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.cdi.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.cdi.SecurityTestUtils.assertSuccess;
import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnFinalMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
