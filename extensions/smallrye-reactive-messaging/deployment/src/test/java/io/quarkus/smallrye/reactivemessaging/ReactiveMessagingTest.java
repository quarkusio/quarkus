package io.quarkus.smallrye.reactivemessaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.channels.ChannelConsumer;
import io.quarkus.smallrye.reactivemessaging.channels.EmitterExample;
import io.quarkus.test.QuarkusUnitTest;

public class ReactiveMessagingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class, ChannelConsumer.class, EmitterExample.class));

    @Inject
    ChannelConsumer channelConsumer;

    @Inject
    EmitterExample emitterExample;

    @Test
    public void testSimpleBean() {
        assertEquals(4, SimpleBean.RESULT.size());
        assertTrue(SimpleBean.RESULT.contains("HELLO"));
        assertTrue(SimpleBean.RESULT.contains("SMALLRYE"));
        assertTrue(SimpleBean.RESULT.contains("REACTIVE"));
        assertTrue(SimpleBean.RESULT.contains("MESSAGE"));
    }

    @Test
    public void testChannelInjection() {
        List<String> consumed = channelConsumer.consume();
        assertEquals(5, consumed.size());
        assertEquals("hello", consumed.get(0));
        assertEquals("with", consumed.get(1));
        assertEquals("SmallRye", consumed.get(2));
        assertEquals("reactive", consumed.get(3));
        assertEquals("message", consumed.get(4));
    }

    @Test
    public void testEmitter() {
        emitterExample.run();
        List<String> list = emitterExample.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

}
