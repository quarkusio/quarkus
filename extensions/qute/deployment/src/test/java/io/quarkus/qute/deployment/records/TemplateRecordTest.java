package io.quarkus.qute.deployment.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class TemplateRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloInt.class, helloWorld.class, hello.class, hello$name.class, hello$top.class)
                    .addAsResource(new StringAsset("Hello {val}!"),
                            "templates/TemplateRecordTest/HelloInt.txt")
                    .addAsResource(new StringAsset("Hello {name}!"),
                            "templates/hello_world.txt")
                    .addAsResource(
                            new StringAsset(
                                    "Hello {#fragment name}{name}{/fragment}::{#fragment top}{index}{/fragment} and {foo}!"),
                            "templates/hello.txt")
                    .addAsResource(new StringAsset("{alpha}:{bravo}:{charlie}"),
                            "templates/TemplateRecordTest/multiParams.txt"));

    @Inject
    Engine engine;

    @Test
    public void testTemplateRecords() throws InterruptedException, ExecutionException {
        HelloInt helloInt = new HelloInt(1);
        assertEquals("Hello 1!", helloInt.render());
        assertEquals("Hello 1!", helloInt.renderAsync().toCompletableFuture().get());
        StringBuilder builder = new StringBuilder();
        helloInt.consume(builder::append).toCompletableFuture().get();
        assertEquals("Hello 1!", builder.toString());
        assertEquals("Hello 1!", helloInt.createUni().await().indefinitely());
        Multi<String> multi = helloInt.createMulti();
        builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        multi.subscribe().with(builder::append, latch::countDown);
        if (latch.await(2, TimeUnit.SECONDS)) {
            assertEquals("Hello 1!", builder.toString());
        } else {
            fail();
        }
        helloInt.setAttribute("foo", true);
        assertNotNull(helloInt.getAttribute("foo"));
        assertEquals(engine.getTimeout(), helloInt.getTimeout());
        assertNull(helloInt.getFragment("bar"));
        CountDownLatch renderedLatch = new CountDownLatch(1);
        helloInt.onRendered(() -> renderedLatch.countDown());
        helloInt.render();
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(helloInt.getTemplate().isFragment());

        assertEquals("Hello Lu!", new helloWorld("Lu").render());

        hello hello = new hello("Ma", "bar", 1);
        assertFalse(hello.getTemplate().isFragment());
        assertEquals("Hello Ma::1 and bar!", hello.render());

        hello$name hello$name = new hello$name("Lu", 1);
        assertTrue(hello$name.getTemplate().isFragment());
        assertEquals("Lu", hello$name.render());

        hello$top hello$top = new hello$top("Lu", 1);
        assertTrue(hello$top.getTemplate().isFragment());
        assertEquals("1", hello$top.render());

        assertEquals("15:true:foo", new multiParams(true, 15, "foo").render());
        assertThrows(IllegalArgumentException.class, () -> new multiParams(false, 50, null));
    }

    record HelloInt(int val) implements TemplateInstance {
    }

    @CheckedTemplate(basePath = "", defaultName = CheckedTemplate.UNDERSCORED_ELEMENT_NAME)
    record helloWorld(String name) implements TemplateInstance {
    }

    @CheckedTemplate(basePath = "")
    record hello(String name, String foo, int index) implements TemplateInstance {
    }

    @CheckedTemplate(basePath = "")
    record hello$name(String name, int index) implements TemplateInstance {
    }

    record multiParams(boolean bravo, int alpha, String charlie) implements TemplateInstance {

        public multiParams {
            if (alpha > 20) {
                throw new IllegalArgumentException();
            }
        }

        public multiParams(String delta) {
            this(false, 1, delta);
        }

        public multiParams(int alpha, boolean bravo, String charlie) {
            this(bravo, alpha, charlie);
            throw new IllegalArgumentException("");
        }
    }

}
