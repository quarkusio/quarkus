package io.quarkus.qute.deployment.varargs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class VarargsMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class)
                    .addAsResource(new StringAsset("{foo.getStr}:{foo.getStr('foo','bar','baz')}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset("{fmt.format('a','b','c')}"),
                            "templates/bar.txt"));

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Test
    public void testVarargs() {
        assertEquals("ok:foo:bar baz", foo.data("foo", new Foo()).render());
        assertEquals(" b a", bar.data("fmt", "%2$2s%1$2s").render());
    }

    @TemplateData(properties = false)
    public static class Foo {

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

        @TemplateExtension
        static String format(String fmt, Object... args) {
            return String.format(fmt, args);
        }

    }

}
