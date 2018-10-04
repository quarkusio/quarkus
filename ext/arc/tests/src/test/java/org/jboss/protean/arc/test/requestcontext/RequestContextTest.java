package org.jboss.protean.arc.test.requestcontext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.enterprise.context.ContextNotActiveException;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class RequestContextTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class, ControllerClient.class);

    @Test
    public void testRequestContext() {

        ArcContainer arc = Arc.container();

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        arc.requestContext().activate();
        assertFalse(Controller.DESTROYED.get());
        Controller controller1 = arc.instance(Controller.class).get();
        Controller controller2 = arc.instance(Controller.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        arc.requestContext().terminate();
        assertTrue(Controller.DESTROYED.get());

        try {
            arc.instance(Controller.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        // Id must be different in a different request
        Controller.DESTROYED.set(false);
        arc.withinRequest(() -> assertNotEquals(controller2Id, arc.instance(Controller.class).get().getId())).run();
        assertTrue(Controller.DESTROYED.get());

        Controller.DESTROYED.set(false);
        assertNotEquals(controller2Id, arc.withinRequest(() -> arc.instance(Controller.class).get().getId()).get());
        assertTrue(Controller.DESTROYED.get());

        // @ActivateRequestContext
        Controller.DESTROYED.set(false);
        ControllerClient client = arc.instance(ControllerClient.class).get();
        assertNotEquals(controller2Id, client.getControllerId());
        assertTrue(Controller.DESTROYED.get());
    }

}
