package io.quarkus.arc.test.bean.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class BeanHasPriorityTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(NoPriority.class, ExplicitPriority0.class,
            ExplicitPriority1.class, Producers.class);

    @Test
    public void test() throws IOException {
        InjectableBean<?> bean = Arc.container().instance(NoPriority.class).getBean();
        assertFalse(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(ExplicitPriority0.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(ExplicitPriority1.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(1, bean.getPriority());

        bean = Arc.container().instance(Foo.class).getBean();
        assertFalse(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(Bar.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(Baz.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(1, bean.getPriority());

        bean = Arc.container().instance(Qux.class).getBean();
        assertFalse(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(Quux.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(0, bean.getPriority());

        bean = Arc.container().instance(Quuux.class).getBean();
        assertTrue(bean.hasPriority());
        assertEquals(1, bean.getPriority());
    }

    static class Foo {
    }

    static class Bar {
    }

    static class Baz {
    }

    static class Qux {
    }

    static class Quux {
    }

    static class Quuux {
    }

    @Singleton
    static class NoPriority {
    }

    @Singleton
    @Priority(0)
    static class ExplicitPriority0 {
    }

    @Singleton
    @Priority(1)
    static class ExplicitPriority1 {
    }

    @Singleton
    static class Producers {
        @Produces
        Foo noPriority = new Foo();

        @Produces
        @Priority(0)
        Bar explicitPriority0 = new Bar();

        @Produces
        @Priority(1)
        Baz explicitPriority1 = new Baz();

        @Produces
        Qux noPriority() {
            return new Qux();
        }

        @Produces
        @Priority(0)
        Quux explicitPriority0() {
            return new Quux();
        }

        @Produces
        @Priority(1)
        Quuux explicitPriority1() {
            return new Quuux();
        }
    }
}
