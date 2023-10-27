package io.quarkus.security.test.cdi;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnObservedMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StartupMethodBean.class, IdentityMock.class, PermitAllEagerAppBean.class,
                            AuthData.class, SecurityTestUtils.class, EagerAppBean.class))
            .assertException(throwable -> {
                String errorMsg = throwable.getMessage();
                Assertions.assertTrue(errorMsg.contains("StartupMethodBean#packagePrivateClassAnnotatedMethod"));
                Assertions.assertTrue(errorMsg.contains("StartupMethodBean#publicInit"));
                Assertions.assertFalse(errorMsg.contains("EagerAppBean"));
                Assertions.assertFalse(errorMsg.contains("packagePrivateInit"));
                Assertions.assertFalse(errorMsg.contains("permittedInit"));
                Assertions.assertFalse(errorMsg.contains("PermitAllEagerAppBean"));
            });

    @Test
    public void test() {
        // must be here to run test
        Assertions.fail();
    }

    @PermissionsAllowed("ignored")
    public static class StartupMethodBean {

        public void publicInit(@Observes StartupEvent event) {
            Assertions.fail("Illegal state - validation should detect secured observed method");
        }

        void packagePrivateInit(@Observes StartupEvent event) {
            // invoked as not intercepted by class level annotation
        }

        @Authenticated
        void packagePrivateClassAnnotatedMethod(@Observes StartupEvent event) {
            Assertions.fail("Illegal state - validation should detect secured observed method");
        }

        @PermitAll
        public void permittedInit(@Observes StartupEvent event) {
            // invoked as not secured
        }

    }

    @RolesAllowed("ignored")
    @Startup
    @ApplicationScoped
    public static class EagerAppBean {
    }

    @PermitAll
    @Startup
    @ApplicationScoped
    public static class PermitAllEagerAppBean {
    }
}
