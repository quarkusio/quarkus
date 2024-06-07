package io.quarkus.qute.deployment.generatedresolvers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.test.QuarkusUnitTest;

public class ImplicitValueResolversTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{name.toUpperCase}"), "templates/hello.html")
                    .addAsResource(new StringAsset("{name}"), "templates/bye.html")
                    .addAsResource(new StringAsset("{name}"), "templates/zero.html"));

    @CheckedTemplate(basePath = "")
    record hello(String name) implements TemplateInstance {
    };

    @CheckedTemplate(basePath = "")
    record bye(String name) implements TemplateInstance {
    };

    @CheckedTemplate(basePath = "")
    record zero(String name) implements TemplateInstance {
    };

    @Inject
    Engine engine;

    @Test
    public void testImplicitResolvers() {
        assertEquals("FOO", new hello("Foo").render());
        assertEquals("Bar", new bye("Bar").render());
        assertEquals("Baz", new zero("Baz").render());
        List<ValueResolver> resolvers = engine.getValueResolvers();
        ValueResolver stringResolver = null;
        for (ValueResolver valueResolver : resolvers) {
            if (valueResolver.getClass().getName().endsWith(ValueResolverGenerator.SUFFIX)
                    && valueResolver.getClass().getName().contains("String")) {
                stringResolver = valueResolver;
            }
        }
        assertNotNull(stringResolver);
    }

}
