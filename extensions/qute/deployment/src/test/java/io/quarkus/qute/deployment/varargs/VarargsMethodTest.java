package io.quarkus.qute.deployment.varargs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class VarargsMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Foo.class)
            .addAsResource(new StringAsset("{foo.getStr}:{foo.getStr('foo','bar','baz')}"), "templates/foo.txt")
            .addAsResource(new StringAsset("{fmt.format('a','b','c')}"), "templates/bar.txt")
            .addAsResource(new StringAsset("{foo.getInt}:{foo.getInt(1)}:{foo.getInt(1,2,3)}"), "templates/baz.txt")
            .addAsResource(new StringAsset(
                    "{cdi:qux.getBoolean(1)}:{cdi:qux.getBoolean(1, true, false)}:{cdi:qux.getBoolean(2, false)}"),
                    "templates/qux.txt"));;

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Inject
    Template baz;

    @Inject
    Template qux;

    @Test
    public void testVarargs() {
        assertEquals("ok:foo:bar baz", foo.data("foo", new Foo()).render());
        assertEquals(" b a", bar.data("fmt", "%2$2s%1$2s").render());
        assertEquals("[]:[1]:[1, 2, 3]", baz.data("foo", new Foo()).render());
        assertEquals("1 []:1 [true, false]:2 [false]", qux.render());
    }

    @TemplateData(properties = false)
    public static class Foo {

        // This one is ignored
        String getStr(String foo) {
            return foo;
        }

        public String getStr(String... args) {
            return "ok";
        }

        // the generated resolver should match any number of string params
        public String getStr(String foo, String... args) {
            return foo + String.format(":%s %s", args[0], args[1]);
        }

        public String getInt(int... args) {
            return Arrays.toString(args);
        }

        @TemplateExtension
        static String format(String fmt, Object... args) {
            return String.format(fmt, args);
        }

    }

    @Named
    @Singleton
    public static class Qux {

        public String getBoolean(int first, Boolean... args) {
            return first + " " + Arrays.toString(args);
        }

    }

}
