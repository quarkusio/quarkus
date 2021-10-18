package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.InstanceImpl;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InjectableInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Washcloth.class, Sponge.class);

    @Test
    public void testDestroy() {
        Alpha alpha = Arc.container().instance(Alpha.class).get();

        assertFalse(Washcloth.DESTROYED.get());
        try (InstanceHandle<Washcloth> handle = alpha.washcloth.getHandle()) {
            InjectableBean<Washcloth> bean = handle.getBean();
            assertNotNull(bean);
            assertFalse(Washcloth.CREATED.get());
            assertEquals(Dependent.class, bean.getScope());
            handle.get().wash();
            assertTrue(Washcloth.CREATED.get());
            // Washcloth has @PreDestroy - the dependent instance should be there
            assertTrue(((InstanceImpl<?>) alpha.washcloth).hasDependentInstances());
        }
        // InstanceHandle#close() should call InstanceHandle#destroy() for @Dependent and should remove the instance from the CC of the Instance
        assertFalse(((InstanceImpl<?>) alpha.washcloth).hasDependentInstances());
        assertTrue(Washcloth.DESTROYED.get());

        assertFalse(Sponge.DESTROYED.get());
        Sponge sponge = alpha.sponge.get();
        assertTrue(Sponge.CREATED.get());
        alpha.sponge.destroy(sponge);
        assertTrue(Sponge.DESTROYED.get());
    }

    @Singleton
    static class Alpha {

        @Inject
        InjectableInstance<Washcloth> washcloth;

        @Inject
        Instance<Sponge> sponge;

        void doSomething() {

        }

    }

    @Dependent
    static class Washcloth {

        static final AtomicBoolean CREATED = new AtomicBoolean(false);
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        void wash() {
        }

        @PostConstruct
        void create() {
            CREATED.set(true);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Singleton
    static class Sponge {

        static final AtomicBoolean CREATED = new AtomicBoolean(false);
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        void wash() {
        }

        @PostConstruct
        void create() {
            CREATED.set(true);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
