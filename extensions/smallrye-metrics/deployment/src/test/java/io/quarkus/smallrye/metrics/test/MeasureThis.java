package io.quarkus.smallrye.metrics.test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.runtime.metrics.MetricsFactory;

public class MeasureThis {
    static LongAdder counter = new LongAdder();
    static LongAdder gauge = new LongAdder();

    static LongAdder runnableCount = new LongAdder();
    static LongAdder callableCount = new LongAdder();
    static LongAdder supplierCount = new LongAdder();

    static Runnable wrappedRunnable;
    static Callable<Long> wrappedCallable;
    static Supplier<Long> wrappedSupplier;

    static MetricsFactory.TimeRecorder timeRecorder;

    static Consumer<MetricsFactory> registerMetrics() {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                metricsFactory.builder("count.me")
                        .buildCounter(MeasureThis.counter::longValue);
                metricsFactory.builder("gauge.supplier")
                        .buildGauge(MeasureThis.gauge::doubleValue);

                MeasureThis.wrappedRunnable = metricsFactory.builder("time.runnable")
                        .buildTimer(new Runnable() {
                            @Override
                            public void run() {
                                runnableCount.increment();
                            }
                        });

                MeasureThis.wrappedCallable = metricsFactory.builder("time.callable")
                        .buildTimer(new Callable<Long>() {
                            @Override
                            public Long call() throws Exception {
                                callableCount.increment();
                                return callableCount.sum();
                            }
                        });

                MeasureThis.wrappedSupplier = metricsFactory.builder("time.supplier")
                        .buildTimer(new Supplier<Long>() {
                            @Override
                            public Long get() {
                                supplierCount.increment();
                                return supplierCount.sum();
                            }
                        });

                MeasureThis.timeRecorder = metricsFactory.builder("time.recorder")
                        .buildTimer();
            }
        };
    }

}
