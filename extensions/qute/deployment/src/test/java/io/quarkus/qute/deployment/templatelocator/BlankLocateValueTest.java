package io.quarkus.qute.deployment.templatelocator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Locate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class BlankLocateValueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CustomLocator.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains("'io.quarkus.qute.Locate#value()' must not be blank"));
                } else {
                    fail("No TemplateException thrown: " + t);
                }
            });

    @Test
    void failValidation() {
        fail();
    }

    @Locate("  ")
    public static class CustomLocator implements TemplateLocator {

        @Override
        public Optional<TemplateLocation> locate(String s) {

            return Optional.empty();
        }

    }

}
