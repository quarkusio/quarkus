package io.quarkus.arc.test.clientproxy.constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import org.junit.Rule;
import org.junit.Test;

public class ClientProxyConstructorGuardTest {

    @Rule
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
