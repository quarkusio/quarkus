package io.quarkus.qute.deployment.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class NamespaceTemplateExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StringExtensions.class, MyEnum.class, EnumExtensions.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() {
        assertEquals("hello:1",
                engine.parse("{str:format('%s:%s','hello', 1)}").render());
        assertEquals("olleh",
                engine.parse("{str:reverse('hello')}").render());
        assertEquals("foolish:olleh",
                engine.parse("{str:foolish('hello')}").render());
        assertEquals("ONE=ONE",
                engine.parse("{MyEnum:ONE}={MyEnum:one}").render());
    }

    @TemplateExtension(namespace = "str")
    public static class StringExtensions {

        static String format(String fmt, Object... args) {
            return String.format(fmt, args);
        }

        static String reverse(String val) {
            return new StringBuilder(val).reverse().toString();
        }

        @TemplateExtension(namespace = "str", matchRegex = "foo.*")
        static String foo(String name, String val) {
            return name + ":" + new StringBuilder(val).reverse().toString();
        }

    }

    public enum MyEnum {

        ONE,
        TWO

    }

    public static class EnumExtensions {

        @TemplateExtension(namespace = "MyEnum", matchName = ANY)
        static MyEnum getVal(String val) {
            return MyEnum.valueOf(val.toUpperCase());
        }

    }

}
