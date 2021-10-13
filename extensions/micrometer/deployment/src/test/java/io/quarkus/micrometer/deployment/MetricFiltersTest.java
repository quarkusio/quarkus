package io.quarkus.micrometer.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.test.QuarkusUnitTest;

public class MetricFiltersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AnnotatedFilter.class, NonAnnotatedFilter.class,
                            MeterFilterProducer.class));

    @Test
    public void testCDIFilters() {
        assertTrue(AnnotatedFilter.COUNT.get() > 0);
        assertTrue(NonAnnotatedFilter.COUNT.get() > 0);
        assertEquals(AnnotatedFilter.COUNT.get(), NonAnnotatedFilter.COUNT.get());
    }

    @Singleton
    public static class AnnotatedFilter implements MeterFilter {

        public static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public Meter.Id map(Meter.Id id) {
            COUNT.incrementAndGet();
            return id;
        }
    }

    public static class NonAnnotatedFilter implements MeterFilter {

        public static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public Meter.Id map(Meter.Id id) {
            COUNT.incrementAndGet();
            return id;
        }
    }

    @Singleton
    public static class MeterFilterProducer {

        @Produces
        @Singleton
        public MeterFilter producedMeterFilter() {
            return new NonAnnotatedFilter();
        }
    }
}
