package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.test.QuarkusUnitTest;

public class InjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("quarkus.qute.suffixes=txt"), "application.properties")
                    .addAsResource(new StringAsset("{this}"), "META-INF/resources/templates/foo.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "META-INF/resources/templates/foo.html")
                    .addAsResource(new StringAsset("{this}"), "META-INF/resources/templates/bars/bar.txt"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testInjection() {
        assertNotNull(simpleBean.engine);
        assertEquals("bar", simpleBean.foo.render("bar"));
        assertEquals("<strong>bar</strong>", simpleBean.foo2.render("bar"));
        assertEquals("bar", simpleBean.bar.render("bar"));
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        Engine engine;

        @Inject
        Template foo;

        @ResourcePath("foo.html")
        Template foo2;

        @ResourcePath("bars/bar")
        Template bar;

    }

}
