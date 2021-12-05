package io.quarkus.smallrye.reactivemessaging.channels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmitterWithBroadcastTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EmitterWithBroadcastExample.class));

    @Inject
    EmitterWithBroadcastExample bean;

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
    }

}
