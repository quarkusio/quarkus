package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamSpecialCharsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("{user_name}"),
                            "templates/CheckedTemplateParamSpecialCharsTest/underscore.txt"));

    @Test
    public void testUnderscore() {
        assertEquals("Alice", Templates.underscore("Alice").render());
    }

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance underscore(@CheckedTemplateParam("user_name") String name);
    }
}
