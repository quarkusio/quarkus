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
import io.quarkus.security.test.cdi.app.interfaces.*;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnInterfaceMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            OverriddenInterfaceWithClassLevelAnnotation.class,
                            BeanOverridingInterfaceWithClassLevelAnnotation.class,
                            OverriddenInterfaceWithMethodLevelAnnotations.class,
                            BeanOverridingInterfaceWithMethodLevelAnnotations.class,
                            BeanWithClassLevelAndInterfaceMethodAnnotation.class,
                            MixedAnnotationsInterface.class,
                            IdentityMock.class,
                            AuthData.class,
                            TestException.class,
                            SecurityTestUtils.class));

    @Inject
    BeanOverridingInterfaceWithClassLevelAnnotation beanOverridingInterfaceWithClassLevelAnnotation;

    @Inject
    BeanOverridingInterfaceWithMethodLevelAnnotations beanOverridingInterfaceWithMethodLevelAnnotations;

    @Inject
    BeanWithClassLevelAndInterfaceMethodAnnotation classLevelAnnotationBean;

    @Test
    public void classLevelAnnotationFromBeanShouldTakePrecedenceOverInterfaceAnnotation() {
        assertSuccess(() -> beanOverridingInterfaceWithClassLevelAnnotation.overriddenMethod(), "this should be permitted",
                ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void methodLevelAnnotationsFromBeanShouldTakePrecedenceOverInterfaceAnnotations() {
        assertSuccess(() -> beanOverridingInterfaceWithMethodLevelAnnotations.overriddenMethod(), "this should be permitted",
                ANONYMOUS, USER, ADMIN);
        assertFailureFor(() -> beanOverridingInterfaceWithMethodLevelAnnotations.otherOverriddenMethod(),
                UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanOverridingInterfaceWithMethodLevelAnnotations.otherOverriddenMethod(),
                ForbiddenException.class, USER, ADMIN);
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
