package io.quarkus.arc.test.clientproxy.finalmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FinalMethodIgnoredTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = Arc.container().instance(Moo.class).get();
        assertTrue(moo instanceof ClientProxy);
        assertEquals(0, moo.getVal());
        assertEquals(10, ((Moo) ((ClientProxy) moo).arc_contextualInstance()).val);
    }

    @ApplicationScoped
    static class Moo {

        private int val;

        @PostConstruct
        void init() {
            this.val = 10;
        }

        // will return 0 if invoked upon a client proxy
        final int getVal() {
            return val;
        }

    }

}
