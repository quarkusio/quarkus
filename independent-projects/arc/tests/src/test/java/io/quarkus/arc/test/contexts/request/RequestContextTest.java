package io.quarkus.arc.test.contexts.request;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.RequestContextController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RequestContextTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Controller.class, ControllerClient.class,
            ContextObserver.class, Boom.class);

    @Test
    public void testRequestContext() {
        Controller.DESTROYED.set(false);
        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate();
        assertFalse(Controller.DESTROYED.get());
        Controller controller1 = arc.instance(Controller.class).get();
        Controller controller2 = arc.instance(Controller.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        requestContext.terminate();
        assertTrue(Controller.DESTROYED.get());

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        // Id must be different in a different request
        Controller.DESTROYED.set(false);
        requestContext.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        requestContext.terminate();
        assertTrue(Controller.DESTROYED.get());

        Controller.DESTROYED.set(false);
        requestContext.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        requestContext.terminate();
        assertTrue(Controller.DESTROYED.get());

        // @ActivateRequestContext
        Controller.DESTROYED.set(false);
        ControllerClient client = arc.instance(ControllerClient.class).get();
        assertNotEquals(controller2Id, client.getControllerId());
        assertTrue(Controller.DESTROYED.get());
    }

    @Test
    public void testRequestContextController() {
        Controller.DESTROYED.set(false);
        ArcContainer arc = Arc.container();
        RequestContextController controller = Arc.container().instance(RequestContextController.class).get();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        controller.activate();
        assertFalse(Controller.DESTROYED.get());
        Controller controller1 = arc.instance(Controller.class).get();
        Controller controller2 = arc.instance(Controller.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        controller.deactivate();
        assertTrue(Controller.DESTROYED.get());

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        // Id must be different in a different request
        Controller.DESTROYED.set(false);
        controller.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        controller.deactivate();
        assertTrue(Controller.DESTROYED.get());

        Controller.DESTROYED.set(false);
        controller.activate();
        assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId());
        controller.deactivate();
        assertTrue(Controller.DESTROYED.get());

        // @ActivateRequestContext
        Controller.DESTROYED.set(false);
        ControllerClient client = arc.instance(ControllerClient.class).get();
        assertNotEquals(controller2Id, client.getControllerId());
        assertTrue(Controller.DESTROYED.get());
    }

    @Test
    public void testRequestContextEvents() {
        // reset counters since other tests might have triggered it already
        ContextObserver.reset();

        // firstly test manual activation
        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(0, ContextObserver.beforeDestroyedObserved);
        assertEquals(0, ContextObserver.destroyedObserved);

        // dummy check that bean is available
        arc.instance(Controller.class).get().getId();

        requestContext.terminate();
        assertEquals(1, ContextObserver.initializedObserved);
        assertEquals(1, ContextObserver.beforeDestroyedObserved);
        assertEquals(1, ContextObserver.destroyedObserved);

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        // now test the same but activate context via interceptor (@ActivateRequestContext)
        arc.instance(ControllerClient.class).get().getControllerId();
        assertEquals(2, ContextObserver.initializedObserved);
        assertEquals(2, ContextObserver.beforeDestroyedObserved);
        assertEquals(2, ContextObserver.destroyedObserved);

        // lastly, use RequestContextController bean to handle the context
        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        RequestContextController controller = arc.instance(RequestContextController.class).get();
        controller.activate();
        assertEquals(3, ContextObserver.initializedObserved);
        assertEquals(2, ContextObserver.beforeDestroyedObserved);
        assertEquals(2, ContextObserver.destroyedObserved);

        // dummy check that bean is available
        arc.instance(Controller.class).get().getId();

        controller.deactivate();
        assertEquals(3, ContextObserver.initializedObserved);
        assertEquals(3, ContextObserver.beforeDestroyedObserved);
        assertEquals(3, ContextObserver.destroyedObserved);

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }
    }

    @Test
    public void testGet() {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            InjectableBean<Boom> boomBean = Arc.container().instance(Boom.class).getBean();
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> requestContext.get(boomBean));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> requestContext.get(boomBean, new CreationalContextImpl<>(boomBean)));
        } finally {
            requestContext.terminate();
        }
    }

    @ApplicationScoped
    public static class Boom {

    }

}
