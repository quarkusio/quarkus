package io.quarkus.qute.deployment.globals;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateGlobalValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(root -> root
            .addClasses(Globals.class).addAsResource(new StringAsset("Hello {user.name}!"), "templates/hello.txt"))
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
                assertTrue(te.getMessage().contains(
                        "Property/method [name] not found on class [java.lang.String] nor handled by an extension method"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    public static class Globals {

        @TemplateGlobal
        static String user = "Fu";

    }

}
