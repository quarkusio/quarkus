package io.quarkus.qute.deployment.globals;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateGlobalInvalidNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(root -> root
            .addClasses(Globals.class).addAsResource(new StringAsset("Hello {user.name}!"), "templates/hello.txt"))
            .assertException(t -> {
                Throwable e = t;
                IllegalArgumentException iae = null;
                while (e != null) {
                    if (e instanceof IllegalArgumentException) {
                        iae = (IllegalArgumentException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(iae);
                assertTrue(iae.getMessage().contains("Invalid global variable name found: -name!"), iae.getMessage());
                assertTrue(iae.getMessage().contains(
                        "supplied by io.quarkus.qute.deployment.globals.TemplateGlobalInvalidNameTest$Globals.user"),
                        iae.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    public static class Globals {

        @TemplateGlobal(name = "-name!")
        static String user = "Fu";

    }

}
