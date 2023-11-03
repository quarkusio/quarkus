package io.quarkus.runtime.metrics;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Extensions can create or register metrics using this factory
 * independent of the enabled metrics provider
 */
public interface MetricsFactory {

    /** A well-known string for MicroProfile metrics provided by the SmallRye Metrics quarkus extension */
    final String MP_METRICS = "smallrye-metrics";

    /** A well-known string for Micrometer metrics provided by the Micrometer Metrics quarkus extension */
    final String MICROMETER = "micrometer";

    /** Registry type or scope. This may not be used by all metrics extensions. */
    public static enum Type {
        APPLICATION,
        BASE,
        VENDOR;
    }

    /**
     * @return true if this factory supports the named metrics system. Arbitrary
     *         strings are allowed. Constants are present for a few.
     * @see #MICROMETER
     * @see #MP_METRICS
     */
    boolean metricsSystemSupported(String name);

    /**
     * @param name The name of the metric (required)
     * @return a fluid builder for registering metrics (default VENDOR type).
     * @see Type
     */
    default MetricBuilder builder(String name) {
        return builder(name, Type.VENDOR);
    };

    /**
     * @param name The name of the metric (required)
     * @param type The scope or type of the metric (optional, may not be used)
     * @return a fluid builder for registering metrics.
     * @see Type
     */
    MetricBuilder builder(String name, Type type);

    interface MetricBuilder {
        /**
         * @param description Description text of the eventual metric (optional).
         * @return The builder with added description.
         */
        MetricBuilder description(String description);

        /**
         * @param key The tag key.
         * @param value The tag value.
         * @return The builder with added tag.
         */
        MetricBuilder tag(String key, String value);

        /**
         * Specify the metric unit (optional)
         *
         * @param unit Base unit of the eventual metric
         * @return The builder with added base unit.
         */
        MetricBuilder unit(String unit);

        /**
         * Register a counter that retrieves its value from a supplier function
         *
         * @param countFunction Function supplying a monotonically increasing number value
         */
        void buildCounter(Supplier<Number> countFunction);

        /**
         * Register a counter that retrieves its value by the applying a function
         * to an object
         *
         * @param obj Object instance to observe
         * @param countFunction Function returning a monotonically increasing value
         */
        <T, R extends Number> void buildCounter(T obj, Function<T, R> countFunction);

        /**
         * Register a gauge that retrieves its value from a supplier function
         *
         * @param gaugeFunction Function supplying number value
         */
        void buildGauge(Supplier<Number> gaugeFunction);

        /**
         * Register a gauge that retrieves its value by applying a function
         * to an object
         *
         * @param obj Object instance to observe
         * @param gaugeFunction Function returning a number value
         */
        <T, R extends Number> void buildGauge(T obj, Function<T, R> gaugeFunction);

        /**
         * @return TimeRecorder to measure passage of time using
         *         incremental updates.
         */
        TimeRecorder buildTimer();

        /**
         * Wrap a {@link Runnable} so that it is timed when invoked.
         *
         * @param f The Runnable to time when it is invoked.
         * @return The wrapped Runnable.
         */
        Runnable buildTimer(Runnable f);

        /**
         * Wrap a {@link Callable} so that it is timed when invoked.
         *
         * @param f The Callable to time when it is invoked.
         * @param <T> The return type of the callable.
         * @return The wrapped callable.
         */
        <T> Callable<T> buildTimer(Callable<T> f);

        /**
         * Wrap a {@link Supplier} so that it is timed when invoked.
         *
         * @param f The {@code Supplier} to time when it is invoked.
         * @param <T> The return type of the {@code Supplier} result.
         * @return The wrapped supplier.
         */
        <T> Supplier<T> buildTimer(Supplier<T> f);
    }

    /**
     * A time recorder that tracks elapsed time using incremental updates
     * using a duration with a specified time unit.
     */
    interface TimeRecorder {
        /**
         * @param amount Duration of a single event being measured by this timer. If the amount is less than 0
         *        the value will be dropped.
         * @param unit Time unit for the amount being recorded.
         */
        void update(long amount, TimeUnit unit);

        /**
         * Updates the statistics kept by the recorder with the specified amount.
         *
         * @param duration Duration of a single event being measured by this timer.
         */
        default void update(Duration duration) {
            update(duration.toNanos(), TimeUnit.NANOSECONDS);
        }
    }
}
