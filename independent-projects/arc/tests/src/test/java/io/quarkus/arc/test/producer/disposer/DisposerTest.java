package io.quarkus.arc.test.producer.disposer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DisposerTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(StringProducer.class, LongProducer.class, BigDecimalProducer.class,
            MyQualifier.class, Pong.class);

    @AfterAll
    public static void afterAll() {
        assertNotNull(BigDecimalProducer.DISPOSED.get());
        assertEquals(1, BigDecimalProducer.DESTROYED.get());
    }

    @Test
    public void testDisposers() {
        InstanceHandle<Long> longHandle = Arc.container().instance(Long.class);
        Long longValue = longHandle.get();
        longHandle.close();
        assertEquals(LongProducer.DISPOSED.get(), longValue);
        // String is only injected in Long disposer
        assertNotNull(StringProducer.DISPOSED.get());
        // Pong should be destroyed when the disposer invocation completes
        assertTrue(Pong.DESTROYED.get());

        // A new instance is created for produce and dispose
        assertEquals(2, StringProducer.DESTROYED.get());
        // Both producer and produced bean are application scoped
        @SuppressWarnings("serial")
        Comparable<BigDecimal> bigDecimal = Arc.container().instance(new TypeLiteral<Comparable<BigDecimal>>() {
        }).get();
        assertEquals(0, bigDecimal.compareTo(BigDecimal.ONE));
    }

    @Singleton
    static class LongProducer {

        static final AtomicReference<Long> DISPOSED = new AtomicReference<>();

        @Dependent
        @Produces
        Long produce() {
            return System.currentTimeMillis();
        }

        void dipose(@Disposes Long value, @MyQualifier String injectedString, Instance<Pong> pongs) {
            assertNotNull(injectedString);
            DISPOSED.set(value);
            pongs.forEach(p -> {
                assertEquals("OK", p.id);
            });
        }

    }

    @Dependent
    static class Pong {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        String id;

        @PostConstruct
        void init() {
            id = "OK";
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Dependent
    static class StringProducer {

        static final AtomicInteger DESTROYED = new AtomicInteger();

        static final AtomicReference<CharSequence> DISPOSED = new AtomicReference<>();

        @MyQualifier
        @Produces
        String produce = toString();

        void dipose(@Disposes @MyQualifier CharSequence value) {
            DISPOSED.set(value);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.incrementAndGet();
        }

    }

    @ApplicationScoped
    static class BigDecimalProducer {

        static final AtomicInteger DESTROYED = new AtomicInteger();

        static final AtomicReference<Object> DISPOSED = new AtomicReference<>();

        @ApplicationScoped
        @Produces
        Comparable<BigDecimal> produce() {
            return BigDecimal.ONE;
        }

        void dipose(@Disposes Comparable<BigDecimal> value) {
            DISPOSED.set(value);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.incrementAndGet();
        }

    }

}
