package io.quarkus.funqy.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.Funq;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class FunqMethodVisibilityTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Hello.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof RuntimeException) {
                    assertTrue(rootCause.getMessage().contains("Method 'greet' annotated with '@Funq'"));
                    assertTrue(rootCause.getMessage().contains("is not public"));
                } else {
                    fail("Non-public method `greet` annotated with `@Funq` wasn't detected: " + t);
                }
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    public static class Hello {

        @Funq
        void greet() {
            // do nothing
        }
    }

}
