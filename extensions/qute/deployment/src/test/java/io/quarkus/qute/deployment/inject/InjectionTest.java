package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class InjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("quarkus.qute.suffixes=txt"), "application.properties")
                    .addAsResource(new StringAsset("{this}"), "templates/foo.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "templates/foo.qute.html")
                    .addAsResource(new StringAsset("{@String foo}{this}"), "templates/bars/bar.txt"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testInjection() {
        assertNotNull(simpleBean.engine);
        assertEquals("bar", simpleBean.foo.render("bar"));
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
