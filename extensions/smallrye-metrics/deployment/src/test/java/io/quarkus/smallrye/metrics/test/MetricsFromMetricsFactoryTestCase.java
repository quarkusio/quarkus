package io.quarkus.smallrye.metrics.test;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsFactory;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsFromMetricsFactoryTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MeasureThis.class));

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry metricRegistry;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        SmallRyeMetricsFactory factory = new SmallRyeMetricsFactory();
        MeasureThis.registerMetrics().accept(factory);

        MeasureThis.counter.increment();
        Counter counter = metricRegistry.counter("count.me");
        Assertions.assertEquals(MeasureThis.counter.longValue(), counter.getCount());

        MeasureThis.gauge.increment();
        MetricID gaugeSupplier = new MetricID("gauge.supplier");
        Gauge<Long> gauge = metricRegistry.getGauges().get(gaugeSupplier);
        Assertions.assertEquals(MeasureThis.gauge.longValue(), (long) gauge.getValue());

        MeasureThis.wrappedRunnable.run();
        SimpleTimer timer = metricRegistry.simpleTimer("time.runnable");
        Assertions.assertEquals(MeasureThis.runnableCount.longValue(), timer.getCount());

        MeasureThis.wrappedCallable.call();
        timer = metricRegistry.simpleTimer("time.callable");
        Assertions.assertEquals(MeasureThis.callableCount.longValue(), timer.getCount());

        MeasureThis.wrappedSupplier.get();
        timer = metricRegistry.simpleTimer("time.supplier");
        Assertions.assertEquals(MeasureThis.supplierCount.longValue(), timer.getCount());

        MeasureThis.timeRecorder.update(10, TimeUnit.MINUTES);
        timer = metricRegistry.simpleTimer("time.recorder");
        Assertions.assertEquals(1, timer.getCount());
    }
}
