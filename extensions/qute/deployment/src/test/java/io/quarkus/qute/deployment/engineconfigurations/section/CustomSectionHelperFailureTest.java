package io.quarkus.qute.deployment.engineconfigurations.section;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class CustomSectionHelperFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(CustomSectionFactory.class, StringProducer.class)
                    .addAsResource(new StringAsset("{#custom bar=1 /}"), "templates/bar.html"))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof TemplateException) {
                    assertTrue(rootCause.getMessage().contains("mandatory section parameters not declared"));
                } else {
                    fail(t);
                }
            });

    @Test
    public void testValidation() {
        fail();
    }

}
