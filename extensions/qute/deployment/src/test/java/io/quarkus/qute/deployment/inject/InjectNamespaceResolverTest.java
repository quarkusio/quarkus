package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
        assertEquals("pong != simple1 and pong != simple1", foo.render());
        assertEquals(1, SimpleBean.DESTROYS.longValue());

        // Test the convenient Qute class
        // By default, the content type is plain text
        assertEquals("pong::simple2::simple2::<br>",
                Qute.fmt("{cdi:hello.ping}::{cdi:simple.ping}::{inject:simple.ping}::{}", "<br>"));
        assertEquals("pong::&lt;br&gt;",
                Qute.fmt("{cdi:hello.ping}::{newLine}").contentType("text/html").data("newLine", "<br>").render());
        assertEquals(2, SimpleBean.DESTROYS.longValue());
    }

    @Named("simple")
    @Dependent
    public static class SimpleBean {

        static final AtomicInteger COUNTER = new AtomicInteger();

        static final LongAdder DESTROYS = new LongAdder();

        private final int id = COUNTER.incrementAndGet();

        public String ping() {
            return "simple" + id;
        }

        @PreDestroy
        void destroy() {
            DESTROYS.increment();
        }

    }

}
