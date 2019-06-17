package io.quarkus.arc.test.instance.destroy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class InstanceDestroyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Washcloth.class, Knight.class);

    @Test
    public void testDestroy() {
        assertFalse(Washcloth.DESTROYED.get());
        Arc.container().instance(Alpha.class).get().doSomething();
        assertTrue(Washcloth.DESTROYED.get());
        // App scoped beans
        Knight knight1 = Arc.container().instance(Knight.class).get();
        String id1 = knight1.getId();
        Arc.container().beanManager().createInstance().destroy(knight1);
        assertTrue(Knight.DESTROYED.get());
        assertNotEquals(id1, Arc.container().instance(Knight.class).get().getId());
    }

    @Singleton
    static class Alpha {

        @Inject
        Instance<Washcloth> instance;

        void doSomething() {
            Washcloth washcloth = instance.get();
            washcloth.wash();
            instance.destroy(washcloth);
        }

    }

    @Dependent
    static class Washcloth {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        void wash() {
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @ApplicationScoped
    static class Knight {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        String id;

        String getId() {
            return id;
        }

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

}
