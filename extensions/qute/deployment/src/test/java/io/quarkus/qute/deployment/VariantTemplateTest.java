package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.VariantTemplate;
import io.quarkus.test.QuarkusUnitTest;

public class VariantTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("{this}"), "templates/foo.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "templates/foo.html"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testRendering() {
        TemplateInstance rendering = simpleBean.foo.instance().data("bar");
        rendering.setAttribute(VariantTemplate.SELECTED_VARIANT, new Variant(null, "text/plain", null));
        assertEquals("bar", rendering.render());
        rendering.setAttribute(VariantTemplate.SELECTED_VARIANT, new Variant(null, "text/html", null));
        assertEquals("<strong>bar</strong>", rendering.render());
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        VariantTemplate foo;

    }

}
