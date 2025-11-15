package io.quarkus.qute.deployment.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.EnumSet;
import java.util.Set;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class NamespaceTemplateExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{#for state in domain:states}{state.foo}{/for}"),
                            "templates/foo.html")
                    .addClasses(StringExtensions.class, MyEnum.class, EnumExtensions.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() {
        assertEquals("hello:1",
                engine.parse("{string:format('%s:%s','hello', 1)}").render());
        assertEquals("1",
                engine.parse("{string:format('%s',1)}").render());
        assertEquals("olleh",
                engine.parse("{string:reverse('hello')}").render());
        try {
            engine.parse("{string:reverse(null)}").render();
            fail();
        } catch (NullPointerException expected) {
        }
        assertEquals("foolish:olleh",
                engine.parse("{string:foolish('hello')}").render());
        assertEquals("ONE=ONE",
                engine.parse("{MyEnum:ONE}={MyEnum:one}").render());
        assertEquals("IN_PROGRESS=0",
                engine.parse("{txPhase:IN_PROGRESS}={txPhase:IN_PROGRESS.ordinal}").render());
        assertEquals("Quark!",
                engine.parse("{string:quark}").render());
        assertEquals("QUARKUS!",
                engine.parse("{string:quarkus}").render());
        assertEquals("openclosed",
                engine.getTemplate("foo").render());
    }

    @TemplateExtension(namespace = "string")
    public static class StringExtensions {

        static String format(String fmt, Object... args) {
            return String.format(fmt, args);
        }

        static String reverse(String val) {
            return new StringBuilder(val).reverse().toString();
        }

        @TemplateExtension(namespace = "string", matchRegex = "foo.*", priority = 5)
        static String foo(String name, String val) {
            return name + ":" + new StringBuilder(val).reverse().toString();
        }

        static String quark() {
            return "Quark!";
        }

        @TemplateExtension(namespace = "string", matchName = ANY, priority = 4)
        static String quarkAny(String key) {
            return key.toUpperCase() + "!";
        }

    }

    public enum MyEnum {
        ONE,
        TWO
    }

    public enum State {
        OPEN,
        CLOSED;

        public String getFoo() {
            return toString().toLowerCase();
        }

    }

    public static class EnumExtensions {

        @TemplateExtension(namespace = "MyEnum", matchName = ANY)
        static MyEnum getVal(String val) {
            return MyEnum.valueOf(val.toUpperCase());
        }

        @TemplateExtension(namespace = "txPhase", matchName = "*")
        static TransactionPhase enumValue(String value) {
            return TransactionPhase.valueOf(value);
        }

        @TemplateExtension(namespace = "domain")
        static Set<State> states() {
            return EnumSet.allOf(State.class);
        }

    }

}
