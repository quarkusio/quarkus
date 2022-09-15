package io.quarkus.arc.test.contexts.request.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ContextNotActiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RequestContextPropagationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SuperController.class, SuperButton.class,
            YetAnotherReqScopedBean.class);

    @Test
    public void testPropagation() {

        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        try {
            arc.instance(SuperController.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate();
        assertFalse(SuperController.DESTROYED.get());
        SuperController controller1 = arc.instance(SuperController.class).get();
        SuperController controller2 = arc.instance(SuperController.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        assertNotNull(controller2.getButton());
        assertTrue(controller2.getButton() == controller1.getButton());

        // Store existing instances
        ContextState state = requestContext.getState();

        // Deactivate but don't destroy
        requestContext.deactivate();

        assertFalse(SuperController.DESTROYED.get());
        assertFalse(SuperButton.DESTROYED.get());

        try {
            // Proxy should not work
            controller1.getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate(state);
        assertEquals(arc.instance(SuperController.class).get().getId(), controller2Id);

        // add req. scoped bean, note that we have already captured the state prior to this
        // we do this to prove that even after capture, newly added beans still get to be propagated
        YetAnotherReqScopedBean yetAnotherReqScopedBean = arc.instance(YetAnotherReqScopedBean.class).get();
        int generatedNumber = yetAnotherReqScopedBean.getRandomNumber();

        // end the context and re-start with originally stored state and assume the bean is part of it
        requestContext.deactivate();
        requestContext.activate(state);

        // check that the bean is not created anew - that means the contextual storage was shared between activations
        assertEquals(arc.instance(YetAnotherReqScopedBean.class).get().getRandomNumber(), generatedNumber);

        requestContext.terminate();
        assertTrue(SuperController.DESTROYED.get());
        assertTrue(SuperButton.DESTROYED.get());
    }

}
