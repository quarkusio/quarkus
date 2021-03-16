package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Metric;

class MpMetadata implements Metadata {

    public static MpMetadata sanitize(Metadata metadata, MetricType type) {
        if (metadata instanceof MpMetadata) {
            return (MpMetadata) metadata;
        }
        return new MpMetadata(metadata, type);
    }

    final String name;
    final MetricType type;
    String description;
    String unit;
    boolean dirty = false;

    MpMetadata(String name, MetricType type) {
        this.name = name;
        this.type = type;
    }

    MpMetadata(Metric annotation, MetricType type) {
        this.name = annotation.name();
        this.description = stringOrNull(annotation.description());
        this.unit = stringOrNull(annotation.unit());
        this.type = type;
    }

    MpMetadata(String name, String description, String unit, MetricType type) {
        this.name = name;
        this.description = stringOrNull(description);
        this.unit = stringOrNull(unit);
        this.type = type;
    }

    MpMetadata(Metadata other, MetricType type) {
        this.type = type;
        this.name = other.getName();
        this.description = other.getDescription().orElse(null);
        this.unit = other.getUnit().orElse(null);
    }

    public boolean mergeSameType(MpMetadata metadata) {
        if (this.type == metadata.type) {
            if (description == null) {
                dirty = true;
                description = stringOrNull(metadata.description);
            }
            if (unit == null) {
                dirty = true;
                unit = stringOrNull(metadata.unit);
            }
            return true;
        }
        return false;
    }

    public boolean mergeSameType(Metadata metadata) {
        if (metadata.getTypeRaw() == MetricType.INVALID || this.type == metadata.getTypeRaw()) {
            if (description == null) {
                dirty = true;
                description = stringOrNull(metadata.getDescription().orElse(null));
            }
            if (unit == null) {
                dirty = true;
                unit = stringOrNull(metadata.getUnit().orElse(null));
            }
            return true;
        }
        return false;
    }

    public boolean mergeSameType(AnnotatedGaugeAdapter annotation) {
        if (this.type == MetricType.GAUGE) {
            if (description == null) {
                dirty = true;
                description = stringOrNull(annotation.description());
            }
            if (unit == null) {
                dirty = true;
                unit = stringOrNull(annotation.baseUnit());
            }
            return true;
        }
        return false;
    }

    public MpMetadata merge(Metric annotation) {
        if (description == null) {
            dirty = true;
            description = stringOrNull(annotation.description());
        }
        if (unit == null) {
            dirty = true;
            unit = stringOrNull(annotation.unit());
        }
        return this;
    }

    public String description() {
        return description;
    }

    public String unit() {
        return unit;
    }

    public boolean cleanDirtyMetadata() {
        boolean precheck = dirty;
        dirty = false;
        return precheck;
    }

    String stringOrNull(String s) {
        if (s == null || s.isEmpty() || "none".equals(s))
            return null;
        return s;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public String getType() {
        return Optional.ofNullable(type).orElse(MetricType.INVALID).name();
    }

    @Override
    public MetricType getTypeRaw() {
        return Optional.ofNullable(type).orElse(MetricType.INVALID);
    }

    @Override
    public Optional<String> getUnit() {
        return Optional.ofNullable(unit);
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MpMetadata that = (MpMetadata) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name
                + "["
                + (description == null ? "" : "description=" + description + " ")
                + (unit == null ? "" : "unit=" + unit + " ")
                + type
                + "]";
    }
}
