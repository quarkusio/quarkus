package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithNoSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotations;
import io.quarkus.security.test.cdi.app.denied.unnanotated.BeanWithSecurityAnnotationsSubBean;
import io.quarkus.security.test.cdi.app.denied.unnanotated.PermitAllBean;
import io.quarkus.security.test.cdi.app.denied.unnanotated.PermitAllSubBean;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CDIAccessDenyUnannotatedTest {

    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    BeanWithSecurityAnnotationsSubBean securityAnnoSubBean;

    @Inject
    PermitAllSubBean permitAllSubBean;

    @Inject
    PermitAllBean permitAllBean;

    @Inject
    BeanWithNoSecurityAnnotations noAnnoBean;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithNoSecurityAnnotations.class,
                            BeanWithSecurityAnnotations.class,
                            BeanWithSecurityAnnotationsSubBean.class,
                            PermitAllBean.class,
                            PermitAllSubBean.class,
                            SecurityTestUtils.class,
                            IdentityMock.class)
                    .addAsResource("application-deny-unannotated.properties",
                            "application.properties"));

    @Test
    public void shouldDenyUnannotated() {
        assertFailureFor(() -> beanWithSecurityAnnotations.unannotated(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanWithSecurityAnnotations.unannotated(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAllowPermitAll() {
        assertSuccess(() -> beanWithSecurityAnnotations.allowed(), "allowed", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldRestrict() {
        assertFailureFor(() -> beanWithSecurityAnnotations.restricted(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanWithSecurityAnnotations.restricted(), ForbiddenException.class, USER);
        assertSuccess(() -> beanWithSecurityAnnotations.restricted(), "accessibleForAdminOnly", ADMIN);
    }

    @Test
    public void shouldNotInheritPermitAll() {
        assertFailureFor(() -> permitAllSubBean.unannotatedInSubclass(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> permitAllSubBean.unannotatedInSubclass(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAllowUnannotatedOnBeanWithNoSecurityAnnotations() {
        assertSuccess(() -> noAnnoBean.unannotated(), "unannotatedOnBeanWithNoAnno", ANONYMOUS, USER, ADMIN);
    }

    @Test
    public void shouldDenyMethodInheritedFromBeanDeniedByDefault() {
        assertFailureFor(() -> securityAnnoSubBean.unannotated(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> securityAnnoSubBean.unannotated(), ForbiddenException.class, USER, ADMIN);
    }

    @Test
    public void shouldAllowClassLevelPermitAll() {
        assertSuccess(() -> permitAllBean.unannotated(), "unannotated", ANONYMOUS, USER, ADMIN);
    }

}
