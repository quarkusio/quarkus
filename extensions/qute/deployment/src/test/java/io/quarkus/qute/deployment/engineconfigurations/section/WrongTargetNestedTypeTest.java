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

public class WrongTargetNestedTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(CustomSection.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains(
                            "Only non-abstract, top-level or static nested classes may be annotated with @EngineConfiguration:"),
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
    public class CustomSection implements SectionHelperFactory<IfSectionHelper> {

        @Override
        public IfSectionHelper initialize(SectionInitContext context) {
            return null;
        }

    }

}
