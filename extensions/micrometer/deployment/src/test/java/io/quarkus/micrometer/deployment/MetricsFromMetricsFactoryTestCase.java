package io.quarkus.micrometer.deployment;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.MicrometerMetricsFactory;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.test.MeasureThis;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsFromMetricsFactoryTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .withApplicationRoot((jar) -> jar
                    .addClass(MeasureThis.class));

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    MicrometerConfig config;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        ((CompositeMeterRegistry) meterRegistry).add(new SimpleMeterRegistry());
        MicrometerMetricsFactory factory = new MicrometerMetricsFactory(config, meterRegistry);
        MeasureThis.registerMetrics().accept(factory);

        Meter count_me = Search.in(meterRegistry).name("count.me").meter();
        Assertions.assertTrue(FunctionCounter.class.isInstance(count_me));
        MeasureThis.counter.increment();
        Assertions.assertEquals(MeasureThis.counter.doubleValue(), ((FunctionCounter) count_me).count());

        Meter gauge_me = Search.in(meterRegistry).name("gauge.supplier").meter();
        Assertions.assertTrue(Gauge.class.isInstance(gauge_me));
        MeasureThis.gauge.increment();
        Assertions.assertEquals(MeasureThis.gauge.doubleValue(), ((Gauge) gauge_me).value());

        Meter runnable = Search.in(meterRegistry).name("time.runnable").meter();
        Assertions.assertTrue(Timer.class.isInstance(runnable));
        MeasureThis.wrappedRunnable.run();
        Assertions.assertEquals(MeasureThis.runnableCount.longValue(), ((Timer) runnable).count(),
                "Runnable invocation count should match");

        Meter callable = Search.in(meterRegistry).name("time.callable").meter();
        Assertions.assertTrue(Timer.class.isInstance(callable));
        MeasureThis.wrappedCallable.call();
        Assertions.assertEquals(MeasureThis.callableCount.longValue(), ((Timer) callable).count(),
                "Callable invocation count should match");

        Meter supplier = Search.in(meterRegistry).name("time.supplier").meter();
        Assertions.assertTrue(Timer.class.isInstance(supplier));
        MeasureThis.wrappedSupplier.get();
        Assertions.assertEquals(MeasureThis.supplierCount.longValue(), ((Timer) supplier).count(),
                "Supplier invocation count should match");

        Meter recorder = Search.in(meterRegistry).name("time.recorder").meter();
        Assertions.assertTrue(Timer.class.isInstance(recorder));
        MeasureThis.timeRecorder.update(10, TimeUnit.MINUTES);
        Assertions.assertEquals(1L, ((Timer) recorder).count(),
                "Recorder invocation count should match");
    }
}
