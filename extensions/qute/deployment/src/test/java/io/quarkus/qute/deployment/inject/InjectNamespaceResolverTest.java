package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.Hello;
import io.quarkus.test.QuarkusUnitTest;

public class InjectNamespaceResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class, Hello.class)
                    .addAsResource(new StringAsset("{inject:hello.ping}"), "templates/foo.html"));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testInjection() {
        assertEquals("pong", simpleBean.foo.render());
    }

    @Dependent
    public static class SimpleBean {

        @Inject
        Template foo;

    }

}
