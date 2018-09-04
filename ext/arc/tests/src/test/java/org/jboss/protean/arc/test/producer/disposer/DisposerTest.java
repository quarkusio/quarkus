package org.jboss.protean.arc.test.producer.disposer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.jboss.protean.arc.test.MyQualifier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class DisposerTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(StringProducer.class, LongProducer.class);

    @Test
    public void testDisposers() {
        InstanceHandle<Long> longHandle = Arc.container().instance(Long.class);
        longHandle.close();
        assertEquals(LongProducer.DISPOSED.get(), longHandle.get());
        // String is only injected in Long disposer
        assertNotNull(StringProducer.DISPOSED.get());
        // A new instance is created for produce and dispose
        assertEquals(2, StringProducer.DESTROYED.get());
    }

    @Singleton
    static class LongProducer {

        static final AtomicReference<Long> DISPOSED = new AtomicReference<>();

        @Dependent
        @Produces
        Long produce() {
            return System.currentTimeMillis();
        }

        void dipose(@Disposes Long value, @MyQualifier String injectedString) {
            assertNotNull(injectedString);
            DISPOSED.set(value);
        }

    }

    @Dependent
    static class StringProducer {

        static final AtomicInteger DESTROYED = new AtomicInteger();

        static final AtomicReference<String> DISPOSED = new AtomicReference<>();

        @MyQualifier
        @Produces
        String produce = toString();

        void dipose(@Disposes @MyQualifier String value) {
            DISPOSED.set(value);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.incrementAndGet();
        }

    }

}
