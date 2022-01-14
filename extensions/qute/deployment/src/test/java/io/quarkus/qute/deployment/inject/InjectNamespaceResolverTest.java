package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.LongAdder;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.Hello;
import io.quarkus.test.QuarkusUnitTest;

public class InjectNamespaceResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class, Hello.class)
                    .addAsResource(
                            new StringAsset(
                                    "{inject:hello.ping} != {inject:simple.ping} and {cdi:hello.ping} != {cdi:simple.ping}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testInjection() {
        assertEquals("pong != simple and pong != simple", foo.render());
        assertEquals(2, SimpleBean.DESTROYS.longValue());

        // Test the convenient Qute class
        // By default, the content type is plain text
        assertEquals("pong::<br>", Qute.fmt("{cdi:hello.ping}::{}", "<br>"));
        assertEquals("pong::&lt;br&gt;",
                Qute.fmt("{cdi:hello.ping}::{newLine}").contentType("text/html").data("newLine", "<br>").render());
    }

    @Named("simple")
    @Dependent
    public static class SimpleBean {

        static final LongAdder DESTROYS = new LongAdder();

        public String ping() {
            return "simple";
        }

        @PreDestroy
        void destroy() {
            DESTROYS.increment();
        }

    }

}
