package io.quarkus.qute.deployment.engineconfigurations.section;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class WrongTargetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(CustomSection.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains(
                            "A class annotated with @EngineConfiguration must implement one of the [io.quarkus.qute.SectionHelperFactory, io.quarkus.qute.ValueResolver, io.quarkus.qute.NamespaceResolver]:"),
                            rootCause.getMessage());
                } else {
                    fail("No TemplateException thrown: " + t);
                }
            });

    @Test
    public void testValidation() {
        fail();
    }

    @EngineConfiguration
    public static class CustomSection {

    }

}
