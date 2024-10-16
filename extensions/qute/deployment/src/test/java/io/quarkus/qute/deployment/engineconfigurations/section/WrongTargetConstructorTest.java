package io.quarkus.qute.deployment.engineconfigurations.section;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.IfSectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongTargetConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(CustomSection.class, StringProducer.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains(
                            "A class annotated with @EngineConfiguration that also implements SectionHelperFactory or ParserHelper must be public and declare a no-args constructor"),
                            rootCause.toString());
                } else {
                    fail("No TemplateException thrown: " + t);
                }
            });

    @Test
    public void testValidation() {
        fail();
    }

    @EngineConfiguration
    public static class CustomSection implements SectionHelperFactory<IfSectionHelper> {

        public CustomSection(String foo) {
        }

        @Override
        public IfSectionHelper initialize(SectionInitContext context) {
            return null;
        }

    }

}
