package io.quarkus.arc.test.clientproxy.constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClientProxyConstructorGuardTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = Arc.container().instance(Moo.class).get();
        assertTrue(moo instanceof ClientProxy);
        assertEquals(10, ((Moo) ((ClientProxy) moo).arc_contextualInstance()).val);
    }

    @ApplicationScoped
    static class Moo {

        private int val;

        public Moo() {
            init();
        }

        void init() {
            val = 10;
        }

    }

}
