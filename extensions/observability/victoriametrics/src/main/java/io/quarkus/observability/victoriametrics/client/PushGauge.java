package io.quarkus.observability.victoriametrics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Like {@link io.prometheus.client.Gauge} but meant to collect samples that
 * can later be pushed rather than scraped.
 */
public class PushGauge extends PushCollector<PushGauge.Child> {

    private PushGauge(Builder b) {
        super(b);
    }

    public static class Builder extends PushCollector.Builder<PushGauge.Builder, PushGauge> {
        @Override
        public PushGauge create() {
            return new PushGauge(this);
        }
    }

    /**
     * Return a Builder to allow configuration of a new PushGauge. Ensures required fields are provided.
     *
     * @param name The name of the metric
     * @param help The help string of the metric
     */
    public static Builder build(String name, String help) {
        return new Builder().name(name).help(help);
    }

    /**
     * Return a Builder to allow configuration of a new Gauge.
     */
    public static Builder build() {
        return new Builder();
    }

    @Override
    protected Child newChild(List<String> labelValues) {
        return new Child(labelValues);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        var samples = children.values()
                .stream()
                .flatMap(child -> child.samples.stream())
                .collect(Collectors.toList());
        return List.of(familySamples(Type.GAUGE, samples));
    }

    public class Child {
        private final List<String> labelValues;
        private final List<MetricFamilySamples.Sample> samples = new ArrayList<>();

        private Child(List<String> labelValues) {
            this.labelValues = labelValues;
        }

        public void add(double value) {
            samples.add(new MetricFamilySamples.Sample(fullname, labelNames, labelValues, value));
        }

        public void add(double value, long timestampMs) {
            samples.add(new MetricFamilySamples.Sample(fullname, labelNames, labelValues, value, timestampMs));
        }
    }
}
