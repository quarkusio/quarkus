package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticBeanInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void testLookupDependentSynthBean() {
        InstanceHandle<MyDependentBean> handle = Arc.container().select(MyDependentBean.class).getHandle();
        try {
            handle.get();
        } catch (Exception e) {
            fail();
        }
        assertNotNull(MyDependentBeanCreator.lookedUp);
        assertNull(MyDependentBeanCreator.lookedUp.getMember());

        try {
            handle.destroy();
        } catch (Exception ignored) {
        }
        assertNull(MyDependentBeanDisposer.lookedUp);
    }

    @Test
    public void testInjectDependentSynthBean() {
        InstanceHandle<MyBean> handle = Arc.container().select(MyBean.class).getHandle();
        try {
            handle.get();
        } catch (Exception e) {
            fail();
        }
        assertNotNull(MyDependentBeanCreator.lookedUp);
        assertNotNull(MyDependentBeanCreator.lookedUp.getMember());
        assertEquals(MyBean.class, MyDependentBeanCreator.lookedUp.getMember().getDeclaringClass());
        assertEquals("dependency", MyDependentBeanCreator.lookedUp.getMember().getName());

        try {
            handle.destroy();
        } catch (Exception ignored) {
        }
        assertNull(MyDependentBeanDisposer.lookedUp);
    }

    @Test
    public void testLookupSingletonSynthBean() {
        try {
            Arc.container().select(MySingletonBean.class).get();
        } catch (Exception ignored) {
        }
        assertNull(MySingletonBeanCreator.lookedUp);
    }

    @Dependent
    public static class MyBean {
        @Inject
        MyDependentBean dependency;
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesise(SyntheticComponents syn) {
            syn.addBean(MyDependentBean.class)
                    .type(MyDependentBean.class)
                    .scope(Dependent.class)
                    .createWith(MyDependentBeanCreator.class)
                    .disposeWith(MyDependentBeanDisposer.class);

            syn.addBean(MySingletonBean.class)
                    .type(MySingletonBean.class)
                    .scope(Singleton.class)
                    .createWith(MySingletonBeanCreator.class);
        }
    }

    // ---

    static class MyDependentBean {
    }

    static class MySingletonBean {
    }

    public static class MyDependentBeanCreator implements SyntheticBeanCreator<MyDependentBean> {
        static InjectionPoint lookedUp = null;

        @Override
        public MyDependentBean create(Instance<Object> lookup, Parameters params) {
            lookedUp = lookup.select(InjectionPoint.class).get();
            return new MyDependentBean();
        }
    }

    public static class MyDependentBeanDisposer implements SyntheticBeanDisposer<MyDependentBean> {
        static InjectionPoint lookedUp = null;

        @Override
        public void dispose(MyDependentBean instance, Instance<Object> lookup, Parameters params) {
            lookedUp = lookup.select(InjectionPoint.class).get();
        }
    }

    public static class MySingletonBeanCreator implements SyntheticBeanCreator<MySingletonBean> {
        static InjectionPoint lookedUp = null;

        @Override
        public MySingletonBean create(Instance<Object> lookup, Parameters params) {
            lookedUp = lookup.select(InjectionPoint.class).get();
            return new MySingletonBean();
        }
    }
}
