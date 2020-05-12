package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

public class NoopMetricRegistry implements MetricRegistry {

    @Override
    public <T extends Metric> T register(String s, T t) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T t) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T t, Tag... tags) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Counter counter(String s) {
        return null;
    }

    @Override
    public Counter counter(String s, Tag... tags) {
        return null;
    }

    @Override
    public Counter counter(MetricID metricID) {
        return null;
    }

    @Override
    public Counter counter(Metadata metadata) {
        return null;
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String s) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String s, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(MetricID metricID) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String s, T t, Function<T, R> function, Tag... tags) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T t, Function<T, R> function) {
        return null;
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T t, Function<T, R> function, Tag... tags) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String s, Supplier<T> supplier, Tag... tags) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        return null;
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {
        return null;
    }

    public Gauge<?> gauge(MetricID metricID, Gauge<?> gauge) {
        return null;
    }

    @Override
    public Histogram histogram(String s) {
        return null;
    }

    @Override
    public Histogram histogram(String s, Tag... tags) {
        return null;
    }

    @Override
    public Histogram histogram(MetricID metricID) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(String s) {
        return null;
    }

    @Override
    public Meter meter(String s, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(MetricID metricID) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(String s) {
        return null;
    }

    @Override
    public Timer timer(String s, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(MetricID metricID) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(MetricID metricID) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Metric getMetric(MetricID metricID) {
        return null;
    }

    @Override
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> asType) {
        return null;
    }

    @Override
    public Counter getCounter(MetricID metricID) {
        return null;
    }

    @Override
    public ConcurrentGauge getConcurrentGauge(MetricID metricID) {
        return null;
    }

    @Override
    public Gauge<?> getGauge(MetricID metricID) {
        return null;
    }

    @Override
    public Histogram getHistogram(MetricID metricID) {
        return null;
    }

    @Override
    public Meter getMeter(MetricID metricID) {
        return null;
    }

    @Override
    public Timer getTimer(MetricID metricID) {
        return null;
    }

    @Override
    public SimpleTimer getSimpleTimer(MetricID metricID) {
        return null;
    }

    @Override
    public Metadata getMetadata(String name) {
        return null;
    }

    @Override
    public boolean remove(String s) {
        return false;
    }

    @Override
    public boolean remove(MetricID metricID) {
        return false;
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {

    }

    @Override
    public SortedSet<String> getNames() {
        return null;
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter filter) {
        return null;
    }

    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> ofType, MetricFilter filter) {
        return null;
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return null;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }
}
