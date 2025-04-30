package io.quarkus.arc.test.defaultbean;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;

public class DefaultBeanPriorityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, FooInterface.class, FooImpl1.class, FooImpl2.class,
            BarInterface.class, BarImpl1.class, BarImpl2.class);

    @Test
    public void testInjection() {
        MyBean myBean = Arc.container().select(MyBean.class).get();
        Assertions.assertEquals(FooImpl2.class.getSimpleName(), myBean.getFoo().ping());
        Assertions.assertEquals(BarImpl2.class.getSimpleName(), myBean.getBar().ping());
    }

    @Test
    public void testSelect() {
        FooInterface foo = CDI.current().select(FooInterface.class).get();
        Assertions.assertEquals(FooImpl2.class.getSimpleName(), foo.ping());

        BarInterface bar = CDI.current().select(BarInterface.class).get();
        Assertions.assertEquals(BarImpl2.class.getSimpleName(), bar.ping());
    }

    @Singleton
    static class MyBean {

        @Inject
        FooInterface foo;

        @Inject
        BarInterface bar;

        FooInterface getFoo() {
            return foo;
        }

        BarInterface getBar() {
            return bar;
        }
    }

    interface FooInterface {
        String ping();
    }

    @ApplicationScoped
    @DefaultBean
    @Priority(10)
    static class FooImpl1 implements FooInterface {

        @Override
        public String ping() {
            return FooImpl1.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @DefaultBean
    @Priority(20)
    static class FooImpl2 implements FooInterface {

        @Override
        public String ping() {
            return FooImpl2.class.getSimpleName();
        }
    }

    interface BarInterface {
        String ping();
    }

    @ApplicationScoped
    @DefaultBean
    // no priority annotation
    static class BarImpl1 implements BarInterface {

        @Override
        public String ping() {
            return BarImpl1.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @DefaultBean
    @Priority(1)
    static class BarImpl2 implements BarInterface {

        @Override
        public String ping() {
            return BarImpl2.class.getSimpleName();
        }
    }
}
