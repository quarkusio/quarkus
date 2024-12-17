package io.quarkus.arc.test.context.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.QuarkusUnitTest;

public class SessionContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class, Client.class));

    @Inject
    Client client;

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testContexts() {
        assertFalse(Arc.container().sessionContext().isActive());
        assertNotNull(client.ping());
        assertTrue(SimpleBean.DESTROYED.get());
        assertFalse(Arc.container().sessionContext().isActive());
        SimpleBean.DESTROYED.set(false);

        ManagedContext sessionContext = Arc.container().sessionContext();
        try {
            sessionContext.activate();
            String id = simpleBean.ping();
            assertEquals(id, client.ping());
            assertFalse(SimpleBean.DESTROYED.get());
        } finally {
            sessionContext.terminate();
        }
        assertTrue(SimpleBean.DESTROYED.get());
    }
}
