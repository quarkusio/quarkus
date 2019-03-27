package io.quarkus.smallrye.reactivemessaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SimpleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class, StreamConsumer.class, StreamEmitter.class));

    @Inject
    StreamConsumer streamConsumer;

    @Inject
    StreamEmitter streamEmitter;

    @Test
    public void testSimpleBean() {
        assertEquals(4, SimpleBean.RESULT.size());
        assertTrue(SimpleBean.RESULT.contains("HELLO"));
        assertTrue(SimpleBean.RESULT.contains("SMALLRYE"));
        assertTrue(SimpleBean.RESULT.contains("REACTIVE"));
        assertTrue(SimpleBean.RESULT.contains("MESSAGE"));
    }

    @Test
    public void testStreamInject() {
        List<String> consumed = streamConsumer.consume();
        assertEquals(5, consumed.size());
        assertEquals("hello", consumed.get(0));
        assertEquals("with", consumed.get(1));
        assertEquals("SmallRye", consumed.get(2));
        assertEquals("reactive", consumed.get(3));
        assertEquals("message", consumed.get(4));
    }

    @Test
    public void testStreamEmitter() {
        streamEmitter.run();
        List<String> list = streamEmitter.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }
}
