package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class NamespaceTemplateExtensionValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{bro:surname}\n"
                                    + "{bro:name.bubu}"),
                            "templates/foo.html")
                    .addClasses(SomeExtensions.class))
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
                assertTrue(te.getMessage().contains("Found incorrect expressions (1)"), te.getMessage());
                assertTrue(te.getMessage().contains("Property/method [bubu] not found on class [java.lang.String]"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    @TemplateExtension(namespace = "bro")
    public static class SomeExtensions {

        static String name() {
            return "bubu";
        }

    }

}
