package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateExtension.TemplateAttribute;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateExtensionAttributeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Extensions.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() {
        assertEquals("bar::en",
                engine.parse("{foo.transform}").data("foo", "bar").setAttribute("locale", Locale.ENGLISH).render());
        assertEquals("OK",
                engine.parse("{foo.myAttr}").instance().setAttribute("myAttribute", "OK").render());
        assertEquals("NULL",
                engine.parse("{foo.myAttr}").render());
        assertEquals("OK",
                engine.parse("{attr:ping}").instance().setAttribute("myAttribute", "OK").render());
    }

    @TemplateExtension
    public static class Extensions {

        static String myAttr(Object any, @TemplateAttribute Object myAttribute) {
            return myAttribute != null ? myAttribute.toString() : "NULL";
        }

        static String transform(String val, @TemplateAttribute("locale") Object loc) {
            return val.toLowerCase() + "::" + loc.toString();
        }

        @TemplateExtension(namespace = "attr")
        static String ping(@TemplateAttribute Object myAttribute) {
            return myAttribute.toString();
        }

    }

}
