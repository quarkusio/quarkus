package io.quarkus.arc.test.instance.destroy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class InstanceDestroyTest {

    @Rule
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

}
