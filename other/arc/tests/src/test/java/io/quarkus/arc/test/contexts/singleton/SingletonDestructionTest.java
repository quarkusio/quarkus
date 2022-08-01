package io.quarkus.arc.test.contexts.singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SingletonDestructionTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependentBean.class, MySingletonBean.class)
            .build();

    @Test
    public void test() {
        assertEquals(0, MyDependentBean.createdCounter.get());
        assertEquals(0, MyDependentBean.destroyedCounter.get());
        assertEquals(0, MySingletonBean.createdCounter.get());
        assertEquals(0, MySingletonBean.destroyedCounter.get());

        InstanceHandle<MyDependentBean> dependentBean = Arc.container().select(MyDependentBean.class).getHandle();
        InstanceHandle<MySingletonBean> singletonBean = Arc.container().select(MySingletonBean.class).getHandle();

        dependentBean.get();
        singletonBean.get();

        assertEquals(1, MyDependentBean.createdCounter.get());
        assertEquals(0, MyDependentBean.destroyedCounter.get());
        assertEquals(1, MySingletonBean.createdCounter.get());
        assertEquals(0, MySingletonBean.destroyedCounter.get());

        dependentBean.destroy();
        singletonBean.destroy();

        assertEquals(1, MyDependentBean.createdCounter.get());
        assertEquals(1, MyDependentBean.destroyedCounter.get());
        assertEquals(1, MySingletonBean.createdCounter.get());
        assertEquals(1, MySingletonBean.destroyedCounter.get());
    }

    // ---

    @Dependent
    static class MyDependentBean {
        static final AtomicInteger createdCounter = new AtomicInteger(0);
        static final AtomicInteger destroyedCounter = new AtomicInteger(0);

        @PostConstruct
        void postConstruct() {
            createdCounter.incrementAndGet();
        }

        @PreDestroy
        void preDestroy() {
            destroyedCounter.incrementAndGet();
        }
    }

    @Singleton
    static class MySingletonBean {
        static final AtomicInteger createdCounter = new AtomicInteger(0);
        static final AtomicInteger destroyedCounter = new AtomicInteger(0);

        @PostConstruct
        void postConstruct() {
            createdCounter.incrementAndGet();
        }

        @PreDestroy
        void preDestroy() {
            destroyedCounter.incrementAndGet();
        }
    }
}
