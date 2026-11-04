package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamEdgeCasesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidTemplates.class)
                    .addAsResource(new StringAsset("Hello {test}!"),
                            "templates/CheckedTemplateParamEdgeCasesTest/test.txt"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                // Should fail due to invalid identifier starting with a number
                assertTrue(te.getMessage().contains("@CheckedTemplateParam value must be a valid Java identifier"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail("Should not be called - invalid @CheckedTemplateParam value should cause build failure");
    }

    @CheckedTemplate
    public static class InvalidTemplates {

        // This should fail because identifier starts with a number
        static native TemplateInstance test(@CheckedTemplateParam("1test") String value);

    }

}
