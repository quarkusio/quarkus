package io.quarkus.observability.victoriametrics.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * Similar to {@link io.prometheus.client.SimpleCollector} but meant to collect samples that
 * can later be pushed rather than scraped.
 */
public abstract class PushCollector<Child> extends Collector {
    protected final String fullname;
    protected final String help;
    protected final String unit;
    protected final List<String> labelNames;

    protected final Map<List<String>, Child> children = new HashMap<List<String>, Child>();
    protected Child noLabelsChild;

    /**
     * Return the Child with the given labels, creating it if needed.
     * <p>
     * Must be passed the same number of labels are were passed to {@link #labelNames}.
     */
    public Child labels(String... labelValues) {
        if (labelValues.length != labelNames.size()) {
            throw new IllegalArgumentException("Incorrect number of labels.");
        }
        for (int i = 0; i < labelValues.length; i++) {
            var label = labelValues[i];
            if (label == null) {
                throw new IllegalArgumentException("Label '" + labelNames.get(i) + "' cannot be null.");
            }
        }
        List<String> key = Arrays.asList(labelValues);
        Child c = children.get(key);
        if (c != null) {
            return c;
        }
        Child c2 = newChild(key);
        Child tmp = children.putIfAbsent(key, c2);
        return tmp == null ? c2 : tmp;
    }

    /**
     * Remove the Child with the given labels.
     * <p>
     * Any references to the Child are invalidated.
     */
    public void remove(String... labelValues) {
        children.remove(Arrays.asList(labelValues));
        initializeNoLabelsChild();
    }

    /**
     * Remove all children.
     * <p>
     * Any references to any children are invalidated.
     */
    public void clear() {
        children.clear();
        initializeNoLabelsChild();
    }

    /**
     * Initialize the child with no labels.
     */
    protected void initializeNoLabelsChild() {
        // Initialize metric if it has no labels.
        if (labelNames.size() == 0) {
            noLabelsChild = labels();
        }
    }

    /**
     * Replace the Child with the given labels.
     * <p>
     * This is intended for advanced uses, in particular proxying metrics
     * from another monitoring system. This allows for callbacks for returning
     * values for {@link Counter} and {@link Gauge} without having to implement
     * a full {@link Collector}.
     * <p>
     * An example with {@link Gauge}:
     *
     * <pre>
     * {@code
     * Gauge.build().name("current_time").help("Current unixtime.").create()
     *         .setChild(new Gauge.Child() {
     *             public double get() {
     *                 return System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
     *             }
     *         }).register();
     * }
     * </pre>
     * <p>
     * Any references any previous Child with these labelValues are invalidated.
     * A metric should be either all callbacks, or none.
     */
    @SuppressWarnings("unchecked")
    public <T extends Collector> T setChild(Child child, String... labelValues) {
        if (labelValues.length != labelNames.size()) {
            throw new IllegalArgumentException("Incorrect number of labels.");
        }
        children.put(Arrays.asList(labelValues), child);
        return (T) this;
    }

    /**
     * Return a new child, workaround for Java generics limitations.
     */
    protected abstract Child newChild(List<String> labelValues);

    protected MetricFamilySamples familySamples(Type type, List<MetricFamilySamples.Sample> samples) {
        return new MetricFamilySamples(fullname, unit, type, help, samples);
    }

    protected PushCollector(Builder<?, ?> b) {
        if (b.name.isEmpty()) {
            throw new IllegalStateException("Name hasn't been set.");
        }
        StringBuilder name = new StringBuilder();
        if (!b.namespace.isEmpty()) {
            name.append(b.namespace).append('_');
        }
        if (!b.subsystem.isEmpty()) {
            name.append(b.subsystem).append('_');
        }
        name.append(b.name);
        unit = b.unit;
        if (!unit.isEmpty()) {
            name.append('_').append(unit);
        }
        fullname = name.toString();
        checkMetricName(fullname);
        if (b.help != null && b.help.isEmpty()) {
            throw new IllegalStateException("Help hasn't been set.");
        }
        help = b.help;
        labelNames = Arrays.asList(b.labelNames);

        for (String n : labelNames) {
            checkMetricLabelName(n);
        }

        if (!b.dontInitializeNoLabelsChild) {
            initializeNoLabelsChild();
        }
    }

    /**
     * Builders let you configure and then create collectors.
     */
    public abstract static class Builder<B extends Builder<B, C>, C extends PushCollector<?>> {
        String namespace = "";
        String subsystem = "";
        String name = "";
        String unit = "";
        String help = "";
        String[] labelNames = new String[] {};
        // Some metrics require additional setup before the initialization can be done.
        boolean dontInitializeNoLabelsChild;

        /**
         * Set the name of the metric. Required.
         */
        @SuppressWarnings("unchecked")
        public B name(String name) {
            this.name = name;
            return (B) this;
        }

        /**
         * Set the subsystem of the metric. Optional.
         */
        @SuppressWarnings("unchecked")
        public B subsystem(String subsystem) {
            this.subsystem = subsystem;
            return (B) this;
        }

        /**
         * Set the namespace of the metric. Optional.
         */
        @SuppressWarnings("unchecked")
        public B namespace(String namespace) {
            this.namespace = namespace;
            return (B) this;
        }

        /**
         * Set the unit of the metric. Optional.
         *
         * @since 0.10.0
         */
        @SuppressWarnings("unchecked")
        public B unit(String unit) {
            this.unit = unit;
            return (B) this;
        }

        /**
         * Set the help string of the metric. Required.
         */
        @SuppressWarnings("unchecked")
        public B help(String help) {
            this.help = help;
            return (B) this;
        }

        /**
         * Set the labelNames of the metric. Optional, defaults to no labels.
         */
        @SuppressWarnings("unchecked")
        public B labelNames(String... labelNames) {
            this.labelNames = labelNames;
            return (B) this;
        }

        /**
         * Return the constructed collector.
         * <p>
         * Abstract due to generics limitations.
         */
        public abstract C create();
    }
}
