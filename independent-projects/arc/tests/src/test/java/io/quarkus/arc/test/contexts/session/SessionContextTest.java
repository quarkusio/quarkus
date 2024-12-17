package io.quarkus.arc.test.contexts.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ContextNotActiveException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;

public class SessionContextTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Controller.class, ContextObserver.class);

    @Test
    public void testSessionContext() {
        Controller.DESTROYED.set(false);
        ArcContainer arc = Arc.container();
        ManagedContext sessionContext = arc.sessionContext();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        sessionContext.activate();
        assertFalse(Controller.DESTROYED.get());
        Controller controller1 = arc.instance(Controller.class).get();
        Controller controller2 = arc.instance(Controller.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        sessionContext.terminate();
        assertTrue(Controller.DESTROYED.get());

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        // Id must be different in a different context
        Controller.DESTROYED.set(false);
        sessionContext.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        sessionContext.terminate();
        assertTrue(Controller.DESTROYED.get());

        Controller.DESTROYED.set(false);
        sessionContext.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        sessionContext.terminate();
        assertTrue(Controller.DESTROYED.get());
    }

    @Test
    public void testSessionContextEvents() {
        // reset counters since other tests might have triggered it already
        ContextObserver.reset();

        // firstly test manual activation
        ArcContainer arc = Arc.container();
        ManagedContext sessionContext = arc.sessionContext();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        sessionContext.activate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(0, ContextObserver.beforeDestroyedObserved);
        assertEquals(0, ContextObserver.destroyedObserved);

        // dummy check that bean is available
        arc.instance(Controller.class).get().getId();

        sessionContext.terminate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(1, ContextObserver.beforeDestroyedObserved);
        assertEquals(1, ContextObserver.destroyedObserved);

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }
    }

}
