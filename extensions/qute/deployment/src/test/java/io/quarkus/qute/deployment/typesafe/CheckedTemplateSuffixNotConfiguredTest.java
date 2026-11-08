package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateSuffixNotConfiguredTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("Hello!"),
                            "templates/CheckedTemplateSuffixNotConfiguredTest/greetings.md"))
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
                assertTrue(te.getMessage().contains(
                        "match the path CheckedTemplateSuffixNotConfiguredTest/greetings but the file suffix is not configured via the quarkus.qute.suffixes property"),
                        te.getMessage());
                assertTrue(te.getMessage().contains("configured suffixes: [qute.html, qute.txt, html, txt]"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance greetings();

    }

}
