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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InjectableInstanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Washcloth.class);

    @Test
    public void testDestroy() {
        assertFalse(Washcloth.DESTROYED.get());

        Arc.container().instance(Alpha.class).get().doSomething();
        assertTrue(Washcloth.DESTROYED.get());
    }

    @Singleton
    static class Alpha {

        @Inject
        InjectableInstance<Washcloth> instance;

        void doSomething() {
            try (InstanceHandle<Washcloth> handle = instance.getHandle()) {
                InjectableBean<Washcloth> bean = handle.getBean();
                assertNotNull(bean);
                assertFalse(Washcloth.CREATED.get());
                assertEquals(Dependent.class, bean.getScope());
                handle.get().wash();
                assertTrue(Washcloth.CREATED.get());
                // Washcloth has @PreDestroy - the dependent instance should be there
                assertTrue(((InstanceImpl<?>) instance).hasDependentInstances());
            }

            // InstanceHandle.destroy() should remove the instance from the CC of the Instance
            assertFalse(((InstanceImpl<?>) instance).hasDependentInstances());
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

}
