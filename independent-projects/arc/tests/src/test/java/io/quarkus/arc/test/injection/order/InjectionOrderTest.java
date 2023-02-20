package io.quarkus.arc.test.injection.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InjectionOrderTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Consumer.class, Dependency.class);

    @Test
    public void test() {
        Consumer consumer = Arc.container().select(Consumer.class).get();

        assertFalse(ConsumerSuperclass.superConstructorInjected); // subclass calls different ctor
        assertTrue(consumer.superFieldInjected());
        assertFalse(ConsumerSuperclass.superInitializerInjected); // overridden in a subclass
        assertTrue(ConsumerSuperclass.superPrivateInitializerInjected); // not overridden, it's private
        assertFalse(ConsumerSuperclass.superPrivateInitializerInjectedBeforeField);

        assertTrue(Consumer.constructorInjected);
        assertTrue(consumer.fieldInjected());
        assertTrue(Consumer.initializerInjected);
        assertFalse(Consumer.initializerInjectedBeforeField);
        assertTrue(Consumer.privateInitializerInjected);

        assertFalse(Consumer.subclassInjectedBeforeSuperclass);
    }

    static class ConsumerSuperclass {
        static boolean superConstructorInjected;
        static boolean superInitializerInjected;
        static boolean superPrivateInitializerInjected;
        static boolean superPrivateInitializerInjectedBeforeField;

        @Inject
        private Dependency dependency;

        ConsumerSuperclass() {
            onSuperInjection();
        }

        @Inject
        ConsumerSuperclass(Dependency dependency) {
            superConstructorInjected = true;
            onSuperInjection();
        }

        @Inject
        void init(Dependency dependency) {
            superInitializerInjected = true;
            onSuperInjection();
        }

        @Inject
        private void privateInit(Dependency dependency) {
            superPrivateInitializerInjected = true;
            if (this.dependency == null) {
                superPrivateInitializerInjectedBeforeField = true;
            }
            onSuperInjection();
        }

        boolean superFieldInjected() {
            return this.dependency != null;
        }

        void onSuperInjection() {
        }
    }

    @Dependent
    static class Consumer extends ConsumerSuperclass {
        static boolean constructorInjected;
        static boolean initializerInjected;
        static boolean initializerInjectedBeforeField;
        static boolean privateInitializerInjected;
        static boolean privateInitializerInjectedBeforeField;

        static boolean subclassInjectedBeforeSuperclass;

        @Inject
        private Dependency dependency;

        @Inject
        Consumer(Dependency dependency) {
            super();
            constructorInjected = true;
        }

        @Inject
        @Override
        void init(Dependency dependency) {
            initializerInjected = true;
            if (this.dependency == null) {
                initializerInjectedBeforeField = true;
            }
        }

        @Inject
        private void privateInit(Dependency dependency) {
            privateInitializerInjected = true;
            if (this.dependency == null) {
                privateInitializerInjectedBeforeField = true;
            }
        }

        boolean fieldInjected() {
            return this.dependency != null;
        }

        @Override
        void onSuperInjection() {
            if (Consumer.initializerInjected || Consumer.privateInitializerInjected || fieldInjected()) {
                Consumer.subclassInjectedBeforeSuperclass = true;
            }
        }
    }

    @Dependent
    static class Dependency {
    }
}
