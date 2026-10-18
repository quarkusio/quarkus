package io.quarkus.arc.test.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Verifies correct behavior in case the same decorator occurs multiple times
 * in the {@code BeanInfo.getNextDecorators()} map. The {@code SubclassGenerator}
 * code generator didn't bother checking, which was fine in Gizmo 1 which didn't
 * check duplicate fields. Gizmo 2 does check, so the {@code SubclassGenerator}
 * has to guard against that.
 */
public class SameDecoratorInMultipleNextChainsTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyInterface.class, MyClass.class, MyDecorator1.class,
            MyDecorator2.class);

    @Test
    public void test() {
        MyInterface instance = Arc.container().instance(MyInterface.class).get();
        assertEquals("d1m1 d2m1 A", instance.method1());
        assertEquals("d1m2 d2m2 B", instance.method2());
    }

    interface MyInterface {
        String method1();

        String method2();
    }

    @ApplicationScoped
    static class MyClass implements MyInterface {
        @Override
        public String method1() {
            return "A";
        }

        @Override
        public String method2() {
            return "B";
        }
    }

    @Priority(10)
    @Decorator
    static class MyDecorator1 implements MyInterface {
        @Inject
        @Delegate
        MyInterface resource;

        @Override
        public String method1() {
            return "d1m1 " + resource.method1();
        }

        @Override
        public String method2() {
            return "d1m2 " + resource.method2();
        }
    }

    @Priority(20)
    @Decorator
    static class MyDecorator2 implements MyInterface {
        @Inject
        @Delegate
        MyInterface resource;

        @Override
        public String method1() {
            return "d2m1 " + resource.method1();
        }

        @Override
        public String method2() {
            return "d2m2 " + resource.method2();
        }
    }
}
