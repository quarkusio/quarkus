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

public class TemplateGlobalDuplicityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Globals.class, NextGlobals.class)
                    .addAsResource(new StringAsset("Hello {user}!"), "templates/hello.txt"))
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
                assertTrue(
                        te.getMessage()
                                .contains("Duplicate global variable defined via @TemplateGlobal for the name [user]"),
                        te.getMessage());
                assertTrue(te.getMessage().contains(
                        "Variable [user] supplied by io.quarkus.qute.deployment.globals.TemplateGlobalDuplicityTest$NextGlobals.user()"),
                        te.getMessage());
                assertTrue(te.getMessage().contains(
                        "Variable [user] supplied by io.quarkus.qute.deployment.globals.TemplateGlobalDuplicityTest$Globals.user"),
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

    @TemplateGlobal
    public static class NextGlobals {

        static String user() {
            return "Lu";
        }
    }

}
