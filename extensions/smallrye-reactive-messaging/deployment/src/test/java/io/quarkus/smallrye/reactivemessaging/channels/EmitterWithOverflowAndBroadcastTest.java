package io.quarkus.smallrye.reactivemessaging.channels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmitterWithOverflowAndBroadcastTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ChannelEmitterWithOverflowAndBroadcast.class));

    @Inject
    ChannelEmitterWithOverflowAndBroadcast bean;

    @Test
    public void testEmitter() {
        bean.run();
        List<String> list = bean.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> list2 = bean.list2();
        assertEquals(3, list2.size());
        assertEquals("a", list2.get(0));
        assertEquals("b", list2.get(1));
        assertEquals("c", list2.get(2));

        List<String> sink1 = bean.sink11();
        assertEquals(3, sink1.size());
        assertEquals("a1", sink1.get(0));
        assertEquals("b1", sink1.get(1));
        assertEquals("c1", sink1.get(2));

        List<String> sink2 = bean.sink12();
        assertEquals(3, sink2.size());
        assertEquals("a1", sink2.get(0));
        assertEquals("b1", sink2.get(1));
        assertEquals("c1", sink2.get(2));

        List<String> sink3 = bean.sink21();
        assertEquals(3, sink3.size());
        assertEquals("a2", sink3.get(0));
        assertEquals("b2", sink3.get(1));
        assertEquals("c2", sink3.get(2));

        List<String> sink4 = bean.sink22();
        assertEquals(3, sink4.size());
        assertEquals("a2", sink4.get(0));
        assertEquals("b2", sink4.get(1));
        assertEquals("c2", sink4.get(2));
    }

}
