package io.quarkus.hibernate.validator.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that validation can be used for early initialized beans that observe {@code @Initialized(ApplicationScoped.class)}
 */
public class ValidatorForEarlyInitializedBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(EagerInitBean.class, ValidatorForEarlyInitializedBeanTest.class));
    @Inject
    EagerInitBean someBean;

    @Test
    void test() {
        Assertions.assertTrue(EagerInitBean.initInvoked);
        try {
            someBean.call(null);
            Assertions.fail();
        } catch (ConstraintViolationException e) {
            // OK, expected
        }

    }

    @ApplicationScoped
    static final class EagerInitBean {

        static boolean initInvoked = false;

        // App scoped is activated very early (compared to observing Startup event)
        void startUp(@Observes @Initialized(ApplicationScoped.class) final Object event) {
            initInvoked = true;
        }

        void call(@NotNull final Object o) {
        }

    }
}
