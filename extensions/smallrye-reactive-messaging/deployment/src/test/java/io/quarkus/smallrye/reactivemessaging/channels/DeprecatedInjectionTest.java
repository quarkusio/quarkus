package io.quarkus.smallrye.reactivemessaging.channels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.SimpleBean;
import io.quarkus.test.QuarkusUnitTest;

public class DeprecatedInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class, DeprecatedChannelConsumer.class, DeprecatedEmitterExample.class));

    @Inject
    DeprecatedChannelConsumer deprecatedChannelConsumer;

    @Inject
    DeprecatedEmitterExample deprecatedEmitterExample;

    @Test
    public void testOldChannelInjection() {
        List<String> consumed = deprecatedChannelConsumer.consume();
        assertEquals(5, consumed.size());
        assertEquals("hello", consumed.get(0));
        assertEquals("with", consumed.get(1));
        assertEquals("SmallRye", consumed.get(2));
        assertEquals("reactive", consumed.get(3));
        assertEquals("message", consumed.get(4));
    }

    @Test
    public void testOldEmitter() {
        deprecatedEmitterExample.run();
        List<String> list = deprecatedEmitterExample.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

}
