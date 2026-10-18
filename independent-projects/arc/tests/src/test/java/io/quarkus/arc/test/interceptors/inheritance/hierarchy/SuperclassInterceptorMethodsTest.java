package io.quarkus.arc.test.interceptors.inheritance.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SuperclassInterceptorMethodsTest {

    static final List<String> LIFECYCLE_CALLBACKS = new CopyOnWriteArrayList<>();

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(AlphaInterceptor.class, Bravo.class, AlphaBinding.class,
            Fool.class);

    @Test
    public void testInterception() {
        LIFECYCLE_CALLBACKS.clear();
        InstanceHandle<Fool> handle = Arc.container().instance(Fool.class);
        Fool fool = handle.get();
        assertEquals("c/b/a/ping/a/b/c", fool.ping());
        assertEquals(4, LIFECYCLE_CALLBACKS.size(), LIFECYCLE_CALLBACKS.toString());
        assertEquals("C", LIFECYCLE_CALLBACKS.get(0));
        assertEquals("A", LIFECYCLE_CALLBACKS.get(1));
        assertEquals("c", LIFECYCLE_CALLBACKS.get(2));
        assertEquals("a", LIFECYCLE_CALLBACKS.get(3));

        LIFECYCLE_CALLBACKS.clear();
        handle.destroy();
        assertEquals(2, LIFECYCLE_CALLBACKS.size(), LIFECYCLE_CALLBACKS.toString());
        assertEquals("b", LIFECYCLE_CALLBACKS.get(0));
        assertEquals("a", LIFECYCLE_CALLBACKS.get(1));
    }

    @AlphaBinding
    @ApplicationScoped
    public static class Fool {

        String ping() {
            return "ping";
        }

    }

}
