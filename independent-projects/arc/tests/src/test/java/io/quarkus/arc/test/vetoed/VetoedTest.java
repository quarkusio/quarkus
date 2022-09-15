package io.quarkus.arc.test.vetoed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Vetoed;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.AbstractList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class VetoedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Seven.class, One.class, VetoedInterceptor.class, Logging.class);

    @Test
    public void testVetoed() {
        ArcContainer arc = Arc.container();
        assertTrue(arc.instance(Seven.class).isAvailable());
        // One is vetoed
        assertFalse(arc.instance(One.class).isAvailable());
        assertFalse(VetoedInterceptor.INTERCEPTED.get());
        assertEquals(Integer.valueOf(7), Integer.valueOf(arc.instance(Seven.class).get().size()));
        // Interceptor is vetoed
        assertFalse(VetoedInterceptor.INTERCEPTED.get());
    }

    @Logging
    @Dependent
    static class Seven extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Override
        public int size() {
            return 7;
        }

    }

    @Vetoed
    @Dependent
    static class One extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(1);
        }

        @Override
        public int size() {
            return 1;
        }

    }

    @Vetoed
    @Priority(1)
    @Interceptor
    @Logging
    static class VetoedInterceptor {

        static final AtomicBoolean INTERCEPTED = new AtomicBoolean(false);

        @AroundInvoke
        public Object aroundInvoke(InvocationContext ctx) throws Exception {
            INTERCEPTED.set(true);
            return ctx.proceed();
        }

    }

}
