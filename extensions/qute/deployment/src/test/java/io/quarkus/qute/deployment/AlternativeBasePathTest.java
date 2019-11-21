package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class AlternativeBasePathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class)
                    .addAsResource(new StringAsset("quarkus.qute.base-path=myPath"), "application.properties")
                    .addAsResource(new StringAsset("{this}"), "META-INF/resources/myPath/foo.txt"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testInjection() {
        assertEquals("bar", simpleBean.foo.render("bar"));
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        Template foo;

    }

}
