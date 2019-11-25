package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.eclipse.microprofile.metrics.*;

public class NoopMetricRegistry extends MetricRegistry {

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
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
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
    public Timer timer(Metadata metadata) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
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
    public Map<MetricID, Metric> getMetrics() {
        return null;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return null;
    }
}
