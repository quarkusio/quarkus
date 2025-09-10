package io.quarkus.arc.test.specialization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class SpecializationUnsupportedTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SomeBean.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(
                        rootCause.getMessage().contains(
                                "Quarkus does not support CDI Full @Specializes annotation"),
                        t.toString());
            });

    @Test
    public void trigger() {
        Assertions.fail();
    }

    @ApplicationScoped
    @Specializes
    public static class SomeBean {

        @Specializes
        @Produces
        String someProducer() {
            return "foo";
        }
    }

}
