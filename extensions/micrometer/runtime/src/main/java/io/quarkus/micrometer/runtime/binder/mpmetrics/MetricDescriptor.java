package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Metric;

import io.micrometer.core.instrument.Tags;

class MetricDescriptor {
    final String name;
    final Tags tags;
    ExtendedMetricID metricId = null;

    MetricDescriptor(String name, Tags tags) {
        this.name = name;
        this.tags = tags;
    }

    MetricDescriptor(String name, String... tags) {
        this.name = name;
        this.tags = Tags.of(tags);
    }

    public MetricDescriptor(Metric annotation) {
        this.name = annotation.name();
        this.tags = Tags.of(annotation.tags());
    }

    public MetricDescriptor(AnnotatedGaugeAdapter adapter) {
        this.name = adapter.name();
        this.tags = Tags.of(adapter.tags());
    }

    public String name() {
        return name;
    }

    public Tags tags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetricDescriptor that = (MetricDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + tags.hashCode();
        return result;
    }

    Tag[] convertTags() {
        List<Tag> mpTags = new ArrayList<>();
        tags.stream().forEach(x -> mpTags.add(new Tag(x.getKey(), x.getValue())));
        return mpTags.toArray(new Tag[0]);
    }

    public String toString() {
        return name + Arrays.asList(tags);
    }

    // Deal with ubiquitous MetricID containing nefarious TreeSet and arbitrary
    // dips into MP Config

    public MetricDescriptor(MetricID metricID) {
        this.name = metricID.getName();
        this.tags = extractTags(metricID.getTagsAsList());
        this.metricId = sanitizeMetricID(metricID, this);
    }

    private Tags extractTags(List<Tag> tagsAsList) {
        List<io.micrometer.core.instrument.Tag> list = new ArrayList<>();
        for (Tag t : tagsAsList) {
            list.add(io.micrometer.core.instrument.Tag.of(t.getTagName(), t.getTagValue()));
        }
        return Tags.of(list);
    }

    public MetricID toMetricID() {
        ExtendedMetricID result = metricId;
        if (result == null) {
            result = metricId = new ExtendedMetricID(this);
        }
        return result;
    }

    public static ExtendedMetricID sanitizeMetricID(MetricID metricID, MetricDescriptor metricDescriptor) {
        if (metricID instanceof ExtendedMetricID) {
            return (ExtendedMetricID) metricID;
        } else {
            return new ExtendedMetricID(metricDescriptor);
        }
    }

    /**
     * Ensure all hashcode/equals comparisons are against MetricDescriptor
     * data (not ancillary MP Config additions).
     */
    static class ExtendedMetricID extends MetricID {
        final MetricDescriptor source;

        public ExtendedMetricID(MetricDescriptor source) {
            super(source.name(), source.convertTags());
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            if (!super.equals(o))
                return false;
            ExtendedMetricID that = (ExtendedMetricID) o;
            return source.equals(that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), source);
        }
    }
}
