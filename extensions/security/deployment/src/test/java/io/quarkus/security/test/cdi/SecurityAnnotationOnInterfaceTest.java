package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.app.TestException;
import io.quarkus.security.test.cdi.app.interfaces.BeanOverridingInterfaceAnnotations;
import io.quarkus.security.test.cdi.app.interfaces.BeanWithClassLevelAnnotation;
import io.quarkus.security.test.cdi.app.interfaces.MixedAnnotationsInterface;
import io.quarkus.security.test.cdi.app.interfaces.OverriddenInterface;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OverriddenInterface.class,
                            BeanOverridingInterfaceAnnotations.class,
                            BeanWithClassLevelAnnotation.class,
                            MixedAnnotationsInterface.class,
                            IdentityMock.class,
                            AuthData.class,
                            TestException.class,
                            SecurityTestUtils.class));

    @Inject
    BeanOverridingInterfaceAnnotations beanOverridingInterface;

    @Inject
    BeanWithClassLevelAnnotation classLevelAnnotationBean;

    @Test
    public void annotationsFromBeanShouldTakePrecedence() {
        assertSuccess(() -> beanOverridingInterface.overriddenMethod(), "this should be permitted", ANONYMOUS, USER, ADMIN);
        assertFailureFor(() -> beanOverridingInterface.otherOverriddenMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanOverridingInterface.otherOverriddenMethod(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void annotationsFromInterfaceShouldTakePrecedenceOnClassLevelAnnotations() {
        assertSuccess(() -> classLevelAnnotationBean.unannotatedMethod(), "accessibleForAdminOnly", ADMIN);
        assertFailureFor(() -> classLevelAnnotationBean.unannotatedMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> classLevelAnnotationBean.unannotatedMethod(), ForbiddenException.class, USER);
        assertFailureFor(() -> classLevelAnnotationBean.denyAllMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> classLevelAnnotationBean.denyAllMethod(), ForbiddenException.class, USER, ADMIN);
    }

}
