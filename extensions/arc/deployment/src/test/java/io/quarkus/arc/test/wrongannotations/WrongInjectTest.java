package io.quarkus.arc.test.wrongannotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.inject.Inject;

import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongInjectTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BeanWithIncorrectInject.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(rootCause.getMessage().contains(
                        "@com.google.inject.Inject declared on io.quarkus.arc.test.wrongannotations.BeanWithIncorrectInject.bm1, use @jakarta.inject.Inject instead"),
                        t.toString());
                assertTrue(rootCause.getMessage().contains(
                        "@javax.inject.Inject declared on io.quarkus.arc.test.wrongannotations.BeanWithIncorrectInject.bm2, use @jakarta.inject.Inject instead"),
                        t.toString());
                assertTrue(rootCause.getMessage().contains(
                        "@com.oracle.svm.core.annotate.Inject declared on io.quarkus.arc.test.wrongannotations.BeanWithIncorrectInject.bm3, use @jakarta.inject.Inject instead"),
                        t.toString());
                assertTrue(rootCause.getMessage().contains(
                        "@javax.inject.Inject declared on io.quarkus.arc.test.wrongannotations.WrongInjectTest.beanManager, use @jakarta.inject.Inject instead"),
                        t.toString());
            });

    @Inject
    BeanManager beanManager;

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        fail();
    }

}
