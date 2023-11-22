package io.quarkus.qute.deployment.globals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateGlobalTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Globals.class, NextGlobals.class)
                    .addAsResource(new StringAsset(
                            "Hello {currentUser}|{global:currentUser}! Your name is {_name}|{global:_name}. You're {age}|{global:age} years old."),
                            "templates/hello.txt"));

    @Inject
    Template hello;

    @Test
    public void testTemplateData() {
        assertEquals("Hello Fu|Fu! Your name is Lu|Lu. You're 40|40 years old.", hello.render());
        assertEquals("Hello Fu|Fu! Your name is Lu|Lu. You're 40|40 years old.",
                Qute.fmt(
                        "Hello {currentUser}|{global:currentUser}! Your name is {_name}|{global:_name}. You're {age}|{global:age} years old.")
                        .render());
        Globals.user = "Hu";
        assertEquals("Hello Hu|Hu! Your name is Lu|Lu. You're 20|20 years old.", hello.render());
        assertEquals("Hello Hu|Hu! Your name is Lu|Lu. You're 20|20 years old.",
                Qute.fmt(
                        "Hello {currentUser}|{global:currentUser}! Your name is {_name}|{global:_name}. You're {age}|{global:age} years old.")
                        .render());

        assertEquals("First color is: RED|RED", Qute.fmt("First color is: {colors[0]}|{global:colors[0]}").render());
    }

    public static class Globals {

        @TemplateGlobal(name = "currentUser")
        static String user = "Fu";

        @TemplateGlobal
        static int age() {
            return user.equals("Fu") ? 40 : 20;
        }

    }

    static enum Color {
        RED,
        GREEN,
        BLUE
    }

    @TemplateGlobal
    public static class NextGlobals {

        // field-level annotation overrides the class-level one
        @TemplateGlobal(name = "_name")
        static final String NAME = user();

        // this method is ignored
        private static String user() {
            return "Lu";
        }

        static Color[] colors() {
            return new Color[] { Color.RED, Color.BLUE };
        }
    }

}
