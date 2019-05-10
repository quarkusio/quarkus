/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.test.requestcontext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.control.RequestContextController;
import org.junit.Rule;
import org.junit.Test;

public class RequestContextTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class, ControllerClient.class, ContextObserver.class);

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

}
