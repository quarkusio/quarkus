package io.quarkus.smallrye.metrics.deployment.spi;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that is picked up by the SmallRye Metrics extension to register metrics required by other extensions.
 */
public final class MetricBuildItem extends MultiBuildItem {

    private final Metadata metadata;
    private final Tag[] tags;
    private final Object implementor;
    private final boolean enabled;
    private String configRootName;

    /**
     * Create a metric build item from the specified metadata and tags.
     * Such metric will be picked up by the Metrics extension and registered in the VENDOR registry.
     * This constructor is applicable to all metric types except gauges.
     *
     * @param metadata The metadata that should be applied to the registered metric
     * @param enabled Whether this metric is enabled
     * @param tags The tags that will be applied to this metric
     * @param configRootName the name of the root configuration of the extension as defined by the <code>@ConfigRoot</code>
     *        annotation.
     */
    public MetricBuildItem(Metadata metadata, boolean enabled, String configRootName, Tag... tags) {
        if (metadata.getTypeRaw() == MetricType.GAUGE) {
            throw new IllegalArgumentException("Gauges require a non-null implementation object");
        }
        this.metadata = metadata;
        this.tags = tags;
        this.implementor = null;
        this.enabled = enabled;
        this.configRootName = configRootName;
    }

    /**
     * Create a metric build item from the specified metadata, tags, and a callable.
     * Such metric will be picked up by the Metrics extension and registered in the VENDOR registry.
     *
     * @param metadata The metadata that should be applied to the registered metric
     * @param implementor The object that implements the metric. It must be an instance of the appropriate
     *        metric class (from the {@link org.eclipse.microprofile.metrics} package).
     *        This is required for gauges and optional for all other metric types.
     * @param enabled Whether this metric is enabled
     * @param tags The tags that will be applied to this metric
     * @param configRootName the name of the root configuration of the extension as defined by the <code>@ConfigRoot</code>
     *        annotation.
     *
     */
    public MetricBuildItem(Metadata metadata, Object implementor, boolean enabled, String configRootName, Tag... tags) {
        if (implementor == null && metadata.getTypeRaw() == MetricType.GAUGE) {
            throw new IllegalArgumentException("Gauges require a non-null implementation object");
        }
        this.metadata = metadata;
        this.tags = tags;
        this.implementor = implementor;
        this.enabled = enabled;
        this.configRootName = configRootName;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Tag[] getTags() {
        return tags;
    }

    public Object getImplementor() {
        return implementor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConfigRootName() {
        return configRootName;
    }
}
