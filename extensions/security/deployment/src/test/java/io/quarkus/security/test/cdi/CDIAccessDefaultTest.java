package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.app.BeanWithSecuredMethods;
import io.quarkus.security.test.cdi.app.SubclassWithDenyAll;
import io.quarkus.security.test.cdi.app.SubclassWithPermitAll;
import io.quarkus.security.test.cdi.app.SubclassWithoutAnnotations;
import io.quarkus.security.test.cdi.app.TestException;
import io.quarkus.security.test.cdi.app.interfaces.BeanImplementingInterfaceWithClassLevelAnnotation;
import io.quarkus.security.test.cdi.app.interfaces.BeanImplementingInterfaceWithMethodLevelAnnotations;
import io.quarkus.security.test.cdi.app.interfaces.InterfaceWithClassLevelAnnotation;
import io.quarkus.security.test.cdi.app.interfaces.InterfaceWithMethodLevelAnnotations;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CDIAccessDefaultTest {

    @Inject
    @Named(BeanWithSecuredMethods.NAME)
    BeanWithSecuredMethods bean;

    @Inject
    @Named(SubclassWithDenyAll.NAME)
    SubclassWithDenyAll denyAllBean;

    @Inject
    SubclassWithPermitAll permitAllBean;

    @Inject
    SubclassWithoutAnnotations unannotatedBean;

    @Inject
    BeanImplementingInterfaceWithMethodLevelAnnotations beanImplementingInterfaceWithMethodLevelAnnotations;

    @Inject
    BeanImplementingInterfaceWithClassLevelAnnotation beanImplementingInterfaceWithClassLevelAnnotation;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            BeanImplementingInterfaceWithClassLevelAnnotation.class,
                            InterfaceWithClassLevelAnnotation.class,
                            BeanImplementingInterfaceWithMethodLevelAnnotations.class,
                            InterfaceWithMethodLevelAnnotations.class,
                            BeanWithSecuredMethods.class,
                            IdentityMock.class,
                            AuthData.class,
                            SubclassWithDenyAll.class,
                            SubclassWithoutAnnotations.class,
                            TestException.class,
                            SubclassWithPermitAll.class,
                            SecurityTestUtils.class));

    @Test
    public void shouldFailToAccessForbidden() {
        assertFailureFor(() -> bean.forbidden(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.forbidden(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAccessAllowed() {
        assertSuccess(() -> bean.unsecuredMethod(), "accessibleForAll", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        assertFailureFor(() -> bean.securedMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.securedMethod(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.securedMethod(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldRestrictAccessToSpecificRoleUni() {
        assertFailureFor(() -> bean.securedMethodUni().await().indefinitely(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> bean.securedMethodUni().await().indefinitely(), ForbiddenException.class, USER);
        assertSuccess(() -> bean.securedMethodUni().await().indefinitely(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldRestrictAccessToSpecificRoleCompletionState() {
        Executable executable = () -> {
            try {
                bean.securedMethodCompletionStage().toCompletableFuture().get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        };
        assertFailureFor(executable, UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(executable, ForbiddenException.class, USER);
        assertSuccess(() -> {
            try {
                return bean.securedMethodCompletionStage().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void testExceptionWrapping() {
        Executable executable = () -> {
            try {
                bean.securedMethodCompletionStageException().toCompletableFuture().get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        };
        assertFailureFor(executable, TestException.class, ADMIN);
    }

    @Test
    public void shouldFailToAccessForbiddenOnClass() {
        assertFailureFor(() -> denyAllBean.noAdditionalConstraints(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> denyAllBean.noAdditionalConstraints(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAccessAllowedMethodOnForbiddenClass() {
        assertSuccess(() -> denyAllBean.allowedMethod(), "allowedOnMethod", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldRestrictAccessToRoleOnMethod() {
        assertFailureFor(() -> denyAllBean.restrictedOnMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> denyAllBean.restrictedOnMethod(), ForbiddenException.class, USER);
        assertSuccess(() -> denyAllBean.restrictedOnMethod(), "restrictedOnMethod", ADMIN);
    }

    @Test
    public void shouldAccessInheritedAllowedMethod() {
        assertSuccess(() -> denyAllBean.unsecuredMethod(), "accessibleForAll", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldFailToAccessForbiddenInheritedMethod() {
        assertFailureFor(() -> denyAllBean.forbidden(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> denyAllBean.forbidden(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAccessAllowedOnClass() {
        assertSuccess(() -> permitAllBean.allowedOnClass(), "allowedOnClass", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldFailToAccessForbiddenMethodOfPermitAllClass() {
        assertFailureFor(() -> permitAllBean.forbiddenOnMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> permitAllBean.forbiddenOnMethod(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldRestrictAccessForRestrictedMethodOfPermitAllClass() {
        assertFailureFor(() -> permitAllBean.restrictedOnMethod(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> permitAllBean.restrictedOnMethod(), ForbiddenException.class, USER);
        assertSuccess(() -> permitAllBean.restrictedOnMethod(), "restrictedOnMethod", ADMIN);
    }

    @Test
    public void shouldAccessInheritedAllowedOnPermitAllClass() {
        assertSuccess(() -> permitAllBean.unsecuredMethod(), "accessibleForAll", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldFailToAccessInheritedForbiddenOnPermitAllClass() {
        assertFailureFor(() -> permitAllBean.forbidden(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> permitAllBean.forbidden(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldFailToAccessInheritedForbiddenOnUnannotatedClass() {
        assertFailureFor(() -> unannotatedBean.noAdditionalConstraints(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> unannotatedBean.noAdditionalConstraints(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldFailToAccessForbiddenOnInterface() {
        assertFailureFor(() -> beanImplementingInterfaceWithMethodLevelAnnotations.forbidden(),
                UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanImplementingInterfaceWithMethodLevelAnnotations.forbidden(),
                ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldRestrictAccessToSpecificRoleOnInterface() {
        assertFailureFor(() -> beanImplementingInterfaceWithMethodLevelAnnotations.securedMethod(),
                UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanImplementingInterfaceWithMethodLevelAnnotations.securedMethod(),
                ForbiddenException.class, USER);
        assertSuccess(() -> beanImplementingInterfaceWithMethodLevelAnnotations.securedMethod(),
                "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldFailToAccessToSpecificRoleOnInterfaceWithClassLevelAnnotation() {
        assertFailureFor(() -> beanImplementingInterfaceWithClassLevelAnnotation.allowedForAdmin(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(() -> beanImplementingInterfaceWithClassLevelAnnotation.allowedForAdmin(), ForbiddenException.class,
                USER);
        assertSuccess(() -> beanImplementingInterfaceWithClassLevelAnnotation.allowedForAdmin(), "accessibleForAdminOnly",
                ADMIN);
    }

}
