package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.Arrays;

import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

public interface AnnotatedGaugeAdapter extends org.eclipse.microprofile.metrics.Gauge<Number>, MeterHolder {
    String name();

    String description();

    String[] tags();

    String baseUnit();

    /** Called by MpRegistryAdapter to register the gauge */
    AnnotatedGaugeAdapter register(MetricDescriptor id, MeterRegistry registry);

    MetricDescriptor getId();

    MpMetadata getMetadata();

    String getTargetName();

    /**
     * Generic base instance of an AnnotatedGaugeAdapter.
     * Generated beans extend this base.
     */
    public static abstract class GaugeAdapterImpl implements AnnotatedGaugeAdapter {
        final MpMetadata metadata;
        final String targetName;
        final String[] tags;

        MetricDescriptor id;
        Gauge gauge;

        public GaugeAdapterImpl(String name, String description, String targetName, String[] tags) {
            this(name, description, targetName, null, tags);
        }

        public GaugeAdapterImpl(String name, String description, String targetName, String baseUnit, String[] tags) {
            this.metadata = new MpMetadata(name, description, baseUnit, MetricType.GAUGE);
            this.targetName = name;
            this.tags = tags;
        }

        /** Called by MpRegistryAdapter to register the gauge */
        public GaugeAdapterImpl register(MetricDescriptor id, MeterRegistry registry) {
            this.id = id;
            if (gauge == null || metadata.cleanDirtyMetadata()) {
                gauge = io.micrometer.core.instrument.Gauge.builder(metadata.name, this, g -> g.getValue().doubleValue())
                        .description(metadata.getDescription())
                        .tags(id.tags())
                        .baseUnit(metadata.getUnit())
                        .strongReference(true)
                        .register(registry);
            }
            return this;
        }

        public String name() {
            return metadata.name;
        }

        public String description() {
            return metadata.description;
        }

        public String baseUnit() {
            return metadata.unit;
        }

        public String[] tags() {
            return tags;
        }

        public Meter getMeter() {
            return gauge;
        }

        public MetricDescriptor getId() {
            return id;
        }

        public MpMetadata getMetadata() {
            return metadata;
        }

        public MetricType getType() {
            return MetricType.GAUGE;
        }

        public String getTargetName() {
            return targetName;
        }

        public String toString() {
            return this.getClass().getName()
                    + "[ name=" + id.name
                    + ", tags=" + Arrays.asList(id.tags)
                    + ", target=" + targetName
                    + "]";
        }
    }
}
