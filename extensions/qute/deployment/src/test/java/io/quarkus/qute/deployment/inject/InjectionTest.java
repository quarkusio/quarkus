package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

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
                    .addAsResource(new StringAsset("{this}"), "templates/bars/bar.txt"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testInjection() {
        assertNotNull(simpleBean.engine);
        assertEquals("bar", simpleBean.foo.render("bar"));
        assertEquals("<strong>bar</strong>", simpleBean.foo2.render("bar"));
        assertEquals("bar", simpleBean.bar.render("bar"));
        assertEquals("bar", simpleBean.barLocation.render("bar"));
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

        @Location("bars/bar")
        Template barLocation;

    }

}
