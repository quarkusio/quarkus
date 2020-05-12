package io.quarkus.smallrye.metrics.runtime;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * DefaultMetadata from MP Metrics API does not have a public default constructor so we use this custom wrapper
 * for passing metric metadata from processor to recorder and reconstructing the original Metadata instance in runtime code.
 */
public class MetadataHolder {

    private String name;

    private MetricType metricType;

    private String description;

    private String displayName;

    private String unit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public static MetadataHolder from(Metadata metadata) {
        MetadataHolder result = new MetadataHolder();
        result.name = metadata.getName();
        result.metricType = metadata.getTypeRaw();
        result.description = metadata.getDescription();
        result.displayName = metadata.getDisplayName();
        result.unit = metadata.getUnit();
        return result;
    }

    public Metadata toMetadata() {
        final MetadataBuilder builder = Metadata.builder()
                .withName(name);
        if (description != null) {
            builder.withDescription(description);
        }
        if (displayName != null) {
            builder.withDisplayName(displayName);
        }
        return builder.withType(metricType)
                .withUnit(unit)
                .build();
    }

}
