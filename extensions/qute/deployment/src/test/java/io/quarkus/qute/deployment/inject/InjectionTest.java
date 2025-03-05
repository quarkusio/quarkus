package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.test.QuarkusUnitTest;

public class InjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("{this}"), "templates/foo.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "templates/foo.qute.html")
                    .addAsResource(new StringAsset("{@String foo}{this}"), "templates/bars/bar.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/foo.1.html")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/foo.1.txt"));

    @Inject
    SimpleBean simpleBean;

    @Inject
    QuteContext quteContext;

    @Inject
    TemplateProducer templateProducer;

    @Test
    public void testInjection() {
        assertNotNull(simpleBean.engine);
        assertTrue(simpleBean.engine.locate("foo.txt").isPresent());
        // foo.qute.html takes precedence
        assertTrue(simpleBean.engine.locate("foo").orElseThrow().getVariant().get().getContentType().equals(Variant.TEXT_HTML));
        assertTrue(simpleBean.engine.locate("foo.html").isEmpty());
        assertEquals("bar",
                simpleBean.foo.instance()
                        .setVariant(Variant.forContentType(Variant.TEXT_PLAIN))
                        .data("bar")
                        .render());
        assertEquals("<strong>bar</strong>", simpleBean.foo2.render("bar"));
        assertEquals("bar", simpleBean.bar.render("bar"));

        // Some operations are only allowed for unambiguous templates
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.getId());
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.getExpressions());
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.getGeneratedId());
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.getParameterDeclarations());
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.getVariant());
        assertThrows(UnsupportedOperationException.class, () -> simpleBean.foo.findExpression(null));
        assertEquals(1, simpleBean.bar.getExpressions().size());
        assertEquals("this", simpleBean.bar.findExpression(e -> e.getParts().size() == 1).getParts().get(0).getName());
        assertEquals("bars/bar.txt", simpleBean.bar.getId());
        assertEquals(1, simpleBean.bar.getParameterDeclarations().size());
        assertEquals("UTF-8", simpleBean.bar.getVariant().get().getEncoding());
        assertNotNull(simpleBean.bar.getGeneratedId());
        assertEquals("foo.qute.html", simpleBean.foo2.getId());
        assertEquals(Variant.TEXT_HTML, simpleBean.foo2.getVariant().get().getContentType());
        List<String> fooVariants = quteContext.getVariants().get("foo");
        // foo -> foo.txt, foo.qute.html
        assertEquals(2, fooVariants.size());
        assertTrue(fooVariants.contains("foo.txt"));
        assertTrue(fooVariants.contains("foo.qute.html"));
        List<String> fooQuteVariants = quteContext.getVariants().get("foo.qute");
        // foo.qute -> foo.qute.html
        assertEquals(1, fooQuteVariants.size());
        assertTrue(fooVariants.contains("foo.qute.html"));

        assertEquals("Hello &lt;strong&gt;Foo&lt;/strong&gt;!", templateProducer.getInjectableTemplate("foo.1").instance()
                .setVariant(Variant.forContentType(Variant.TEXT_HTML))
                .data("name", "<strong>Foo</strong>")
                .render());
        assertEquals("Hello <strong>Foo</strong>!", templateProducer.getInjectableTemplate("foo.1").instance()
                .setVariant(Variant.forContentType(Variant.TEXT_PLAIN))
                .data("name", "<strong>Foo</strong>")
                .render());
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        Engine engine;

        @Inject
        Template foo;

        @Location("foo.qute.html")
        Template foo2;

        @Location("bars/bar")
        Template bar;

    }

}