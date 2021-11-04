package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateDoNotRequireTypeSafeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("Hello {name}!{any}"),
                            "templates/CheckedTemplateDoNotRequireTypeSafeTest/hola.txt"));

    @Test
    public void testValidation() {
        assertEquals("Hello Ondrej!!", Templates.hola("Ondrej").data("any", "!").render());
    }

    @CheckedTemplate(requireTypeSafeExpressions = false)
    static class Templates {

        static native TemplateInstance hola(String name);

    }

}
