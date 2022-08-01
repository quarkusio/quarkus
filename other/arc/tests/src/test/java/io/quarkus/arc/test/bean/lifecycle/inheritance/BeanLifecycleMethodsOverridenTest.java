package io.quarkus.arc.test.bean.lifecycle.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests the behavior of overrided pre-destroy and post construct methods in beans.
 * Inspired by CDI TCK test {@code OverridenLifecycleCallbackInterceptorTest}
 */
public class BeanLifecycleMethodsOverridenTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Bird.class, Eagle.class, Falcon.class);

    @Test
    public void testOverridenMethodWithNoAnnotation() {
        resetAll();
        InstanceHandle<Falcon> falconInstanceHandle = Arc.container().instance(Falcon.class);
        falconInstanceHandle.get().ping();
        falconInstanceHandle.destroy();
        assertEquals(0, Falcon.getInitCalled().get());
        assertEquals(0, Falcon.getDestroyCalled().get());
        assertEquals(0, Bird.getInitCalled().get());
        assertEquals(0, Bird.getDestroyCalled().get());
    }

    @Test
    public void testOverridenMethodWithLifecycleAnnotation() {
        resetAll();
        InstanceHandle<Eagle> eagleInstanceHandle = Arc.container().instance(Eagle.class);
        eagleInstanceHandle.get().ping();
        eagleInstanceHandle.destroy();
        assertEquals(1, Eagle.getInitCalled().get());
        assertEquals(1, Eagle.getDestroyCalled().get());
        assertEquals(0, Bird.getInitCalled().get());
        assertEquals(0, Bird.getDestroyCalled().get());
    }

    private void resetAll() {
        Bird.reset();
        Falcon.reset();
        Eagle.reset();
    }
}
