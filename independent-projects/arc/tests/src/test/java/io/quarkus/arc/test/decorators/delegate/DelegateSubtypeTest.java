package io.quarkus.arc.test.decorators.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DelegateSubtypeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(A.class, B.class, C.class, D.class);

    @Test
    public void testDecoration() {
        A a = Arc.container().instance(A.class).get();
        assertEquals(2, a.ping());
    }

    public interface A {

        int ping();

    }

    @ApplicationScoped
    public static class B implements A {

        @Override
        public int ping() {
            return 0;
        }
    }

    @Decorator
    public static class C implements A {

        private final A a;

        @Inject
        public C(@Delegate A a) {
            this.a = a;
        }

        @Override
        public int ping() {
            return a.ping() + 1;
        }

    }

    @Decorator
    public static class D implements A {

        private final A a;

        // The delegate type is B!
        @Inject
        public D(@Delegate B b) {
            this.a = b;
        }

        @Override
        public int ping() {
            return a.ping() + 1;
        }

    }
}
