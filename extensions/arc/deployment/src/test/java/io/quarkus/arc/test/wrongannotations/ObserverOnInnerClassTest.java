package io.quarkus.arc.test.wrongannotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ObserverOnInnerClassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IgnoredClass.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertTrue(
                        rootCause.getMessage().contains(
                                "INNER class io.quarkus.arc.test.wrongannotations.ObserverOnInnerClassTest$IgnoredClass"),
                        t.toString());
            });

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        fail();
    }

    class IgnoredClass {

        void observe(@Observes String event) {
        }

    }

}
