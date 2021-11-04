package io.quarkus.qute.deployment.generatedresolvers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.generator.ValueResolverGenerator;
import io.quarkus.test.QuarkusUnitTest;

public class HierarchyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, Bar.class)
                    .addAsResource(new StringAsset("{foo.name}"), "templates/test.html"));

    @Inject
    Template test;

    @Inject
    Engine engine;

    @Test
    public void testGeneratedResolvers() {
        List<ValueResolver> resolvers = engine.getValueResolvers();
        ValueResolver fooResolver = null;
        ValueResolver barResolver = null;
        for (ValueResolver valueResolver : resolvers) {
            if (valueResolver.getClass().getName().endsWith(ValueResolverGenerator.SUFFIX)
                    && valueResolver.getClass().getName().contains("Foo")) {
                fooResolver = valueResolver;
            }
            if (valueResolver.getClass().getName().endsWith(ValueResolverGenerator.SUFFIX)
                    && valueResolver.getClass().getName().contains("Bar")) {
                barResolver = valueResolver;
            }
        }
        assertNotNull(fooResolver);
        assertNotNull(barResolver);
        assertTrue(barResolver.getPriority() > fooResolver.getPriority(), "Bar resolver priority " + barResolver.getPriority()
                + " is not higher than Foo resolver priority " + fooResolver.getPriority());
        assertEquals("bar", test.data("foo", new Bar()).render());
    }

    @TemplateData
    public static class Foo {

        public String getName() {
            return "foo";
        }

    }

    @TemplateData
    public static class Bar extends Foo {

        @Override
        public String getName() {
            return "bar";
        }

    }

}
