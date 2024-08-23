package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticBeanWithLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependentBean.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        assertEquals(0, MyPojo.createdCounter.get());
        assertEquals(0, MyPojo.destroyedCounter.get());
        assertEquals(0, MyPojoCreator.counter.get());
        assertEquals(0, MyPojoDisposer.counter.get());
        assertEquals(0, MyDependentBean.createdCounter.get());
        assertEquals(0, MyDependentBean.destroyedCounter.get());

        InstanceHandle<MyPojo> bean = Arc.container().select(MyPojo.class).getHandle();
        assertEquals("Hello!", bean.get().hello());

        assertEquals(1, MyPojo.createdCounter.get());
        assertEquals(0, MyPojo.destroyedCounter.get());
        assertEquals(1, MyPojoCreator.counter.get());
        assertEquals(0, MyPojoDisposer.counter.get());
        assertEquals(1, MyDependentBean.createdCounter.get());
        assertEquals(0, MyDependentBean.destroyedCounter.get());

        bean.destroy();

        assertEquals(1, MyPojo.createdCounter.get());
        assertEquals(1, MyPojo.destroyedCounter.get());
        assertEquals(1, MyPojoCreator.counter.get());
        assertEquals(1, MyPojoDisposer.counter.get());
        assertEquals(2, MyDependentBean.createdCounter.get());
        assertEquals(2, MyDependentBean.destroyedCounter.get());
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesise(SyntheticComponents syn) {
            syn.addBean(MyPojo.class)
                    .type(MyPojo.class)
                    .scope(Dependent.class)
                    .createWith(MyPojoCreator.class)
                    .disposeWith(MyPojoDisposer.class);
        }
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

    static class MyPojo {
        static final AtomicInteger createdCounter = new AtomicInteger(0);
        static final AtomicInteger destroyedCounter = new AtomicInteger(0);

        public MyPojo() {
            createdCounter.incrementAndGet();
        }

        public String hello() {
            return "Hello!";
        }

        public void destroy() {
            destroyedCounter.incrementAndGet();
        }
    }

    public static class MyPojoCreator implements SyntheticBeanCreator<MyPojo> {
        static final AtomicInteger counter = new AtomicInteger();

        @Override
        public MyPojo create(Instance<Object> lookup, Parameters params) {
            counter.incrementAndGet();

            lookup.select(MyDependentBean.class).get();

            return new MyPojo();
        }
    }

    public static class MyPojoDisposer implements SyntheticBeanDisposer<MyPojo> {
        static final AtomicInteger counter = new AtomicInteger();

        @Override
        public void dispose(MyPojo instance, Instance<Object> lookup, Parameters params) {
            counter.incrementAndGet();

            lookup.select(MyDependentBean.class).get();

            instance.destroy();
        }
    }
}
