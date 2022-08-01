package io.quarkus.arc.test.contexts.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that lifecycle events for request scoped are fired even though no actual request scoped bean exists.
 */
public class LifecycleEventsWithNoBeanTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ContextObserver.class);

    @Test
    public void testEventsWithNoReqContextBean() {
        // reset counters since other tests might have triggered it already
        ContextObserver.reset();

        // manually activate/deactivate context and assert observers were triggered
        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        requestContext.activate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(0, ContextObserver.beforeDestroyedObserved);
        assertEquals(0, ContextObserver.destroyedObserved);

        requestContext.terminate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(1, ContextObserver.beforeDestroyedObserved);
        assertEquals(1, ContextObserver.destroyedObserved);
    }

}
