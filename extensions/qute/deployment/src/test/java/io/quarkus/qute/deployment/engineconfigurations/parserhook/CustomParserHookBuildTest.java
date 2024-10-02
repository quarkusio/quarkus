package io.quarkus.qute.deployment.engineconfigurations.parserhook;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.ParserHelper;
import io.quarkus.qute.ParserHook;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class CustomParserHookBuildTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(CustomParserHook.class, Foo.class)
                            .addAsResource(new StringAsset("{foo.bar}"), "templates/foo.html"))
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
                assertTrue(te.getMessage().contains("{foo.bar}"), te.getMessage());
            });;

    @Test
    public void test() {
        fail();
    }

    @EngineConfiguration
    public static class CustomParserHook implements ParserHook {

        @Override
        public void beforeParsing(ParserHelper helper) {
            if (helper.getTemplateId().contains("foo")) {
                helper.addParameter("foo", Foo.class.getName());
            }
        }

    }

    public static class Foo {

        // package-private method is ignored
        String bar() {
            return null;
        }

    }

}
