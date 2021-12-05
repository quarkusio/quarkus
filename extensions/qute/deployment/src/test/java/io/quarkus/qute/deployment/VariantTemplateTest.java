package io.quarkus.qute.deployment;

import static io.quarkus.qute.TemplateInstance.SELECTED_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class VariantTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("{this}"), "templates/foo.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "templates/foo.html"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testRendering() {
        TemplateInstance instance = simpleBean.foo.instance().data("bar");
        instance.setAttribute(SELECTED_VARIANT, Variant.forContentType("text/plain"));
        assertEquals("bar", instance.render());
        instance.setAttribute(SELECTED_VARIANT, Variant.forContentType("text/html"));
        assertEquals("<strong>bar</strong>", instance.render());
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        Template foo;

    }

}
