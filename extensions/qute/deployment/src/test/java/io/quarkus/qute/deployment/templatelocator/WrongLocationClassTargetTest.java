package io.quarkus.qute.deployment.templatelocator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Locate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.Variant;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongLocationClassTargetTest {

    private static final String TEMPLATE_LOCATION = "/path/to/my/custom/template/basic.html";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CustomLocator.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains(
                            "Classes annotated with 'io.quarkus.qute.Locate' must implement 'io.quarkus.qute.TemplateLocator'"));
                } else {
                    fail("No TemplateException thrown: " + t);
                }
            });

    @Test
    void failValidation() {
        fail();
    }

    @Locate(TEMPLATE_LOCATION)
    public static class CustomLocator {

        public Optional<TemplateLocation> locate(String s) {

            if (s.equals(TEMPLATE_LOCATION)) {
                return Optional.of(new TemplateLocation() {

                    @Override
                    public Reader read() {
                        return new StringReader("Basic {name}!");
                    }

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }
                });
            }
            return Optional.empty();
        }

    }

}
