package io.quarkus.qute.deployment.engineconfigurations.parserhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.ParserHelper;
import io.quarkus.qute.ParserHook;
import io.quarkus.test.QuarkusUnitTest;

public class CustomParserHookRuntimeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(CustomParserHook.class)
                            .addAsResource(new StringAsset("{foo}"), "templates/foo.html"));

    @Inject
    Engine engine;

    @Test
    public void testParserHook() {
        assertEquals("42", engine.getTemplate("foo").data("bar", 42).render());
    }

    @EngineConfiguration
    public static class CustomParserHook implements ParserHook {

        @Inject
        Engine engine;

        @Override
        public void beforeParsing(ParserHelper helper) {
            if (helper.getTemplateId().contains("foo") && engine != null) {
                helper.addContentFilter(c -> "{bar}");
            }
        }

    }

}
