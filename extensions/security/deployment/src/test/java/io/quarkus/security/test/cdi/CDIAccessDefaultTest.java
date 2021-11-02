package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.app.BeanWithSecuredMethods;
import io.quarkus.security.test.cdi.app.SubclassWithDenyAll;
import io.quarkus.security.test.cdi.app.SubclassWithPermitAll;
import io.quarkus.security.test.cdi.app.SubclassWithoutAnnotations;
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

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanWithSecuredMethods.class,
                            IdentityMock.class,
                            AuthData.class,
                            SubclassWithDenyAll.class,
                            SubclassWithoutAnnotations.class,
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
}
