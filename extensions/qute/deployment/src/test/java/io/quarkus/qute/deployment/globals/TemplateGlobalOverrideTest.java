package io.quarkus.qute.deployment.globals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateGlobalOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Globals.class, User.class)
                    .addAsResource(new StringAsset(
                            // Note that we need to override the param declaration as well
                            "{@io.quarkus.qute.deployment.globals.User user}Hello {user.name}!"),
                            "templates/hello.txt")
                    .addAsResource(new StringAsset(
                            // We don't need to override the param declaration for @CheckedTemplate
                            "Hello {user.name}!"),
                            "templates/foo/hello.txt"));

    @CheckedTemplate(basePath = "foo")
    static class Templates {

        static native TemplateInstance hello(User user);

    }

    @Inject
    Template hello;

    @Test
    public void testOverride() {
        assertEquals("Hello Morna!", hello.data("user", new User("Morna")).render());
        assertEquals("Hello Morna!", Templates.hello(new User("Morna")).render());
    }

    public static class Globals {

        // this global variable is overridden in both templates
        @TemplateGlobal
        static String user = "Fu";

    }

}
