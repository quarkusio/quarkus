package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplatePrimitiveTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Templates.class).addAsResource(new StringAsset("Hello {val}!"),
                    "templates/CheckedTemplatePrimitiveTypeTest/integers.txt"));

    @Test
    public void testPrimitiveParamBinding() {
        assertEquals("Hello 1!", Templates.integers(1).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance integers(int val);

    }

}
