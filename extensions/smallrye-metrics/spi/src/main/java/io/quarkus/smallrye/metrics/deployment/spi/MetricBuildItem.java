package io.quarkus.smallrye.metrics.deployment.spi;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that is picked up by the SmallRye Metrics extension to register metrics required by other extensions.
 */
@Deprecated
public final class MetricBuildItem extends MultiBuildItem {

    private final Metadata metadata;
    private final Tag[] tags;
    private final Object implementor;
    private final boolean enabled;
    private final String configRootName;
    private final MetricRegistry.Type registryType;

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
        this.registryType = MetricRegistry.Type.VENDOR;
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
        this.registryType = MetricRegistry.Type.VENDOR;
    }

    /**
     * Create a metric build item from the specified metadata, tags, a callable, and scope.
     * Such metric will be picked up by the Metrics extension and registered in the registry for the desired scope.
     *
     * @param metadata The metadata that should be applied to the registered metric
     * @param implementor The object that implements the metric. It must be an instance of the appropriate
     *        metric class (from the {@link org.eclipse.microprofile.metrics} package).
     *        This is required for gauges and optional for all other metric types.
     * @param enabled Whether this metric is enabled
     * @param tags The tags that will be applied to this metric
     * @param configRootName the name of the root configuration of the extension as defined by the <code>@ConfigRoot</code>
     *        annotation.
     * @param registryType Registry where the metric should be placed
     *
     */
    public MetricBuildItem(Metadata metadata, Object implementor, boolean enabled, String configRootName,
            MetricRegistry.Type registryType, Tag... tags) {
        if (implementor == null && metadata.getTypeRaw() == MetricType.GAUGE) {
            throw new IllegalArgumentException("Gauges require a non-null implementation object");
        }
        this.metadata = metadata;
        this.tags = tags;
        this.implementor = implementor;
        this.enabled = enabled;
        this.configRootName = configRootName;
        this.registryType = registryType;
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

    public MetricRegistry.Type getRegistryType() {
        return registryType;
    }

    public static class Builder {

        private Metadata metadata;
        private Tag[] tags;
        private Object implementor;
        private boolean enabled;
        private String configRootName;
        private MetricRegistry.Type registryType;

        public Builder() {
            this.registryType = MetricRegistry.Type.VENDOR;
            this.enabled = true;
            this.tags = new Tag[0];
        }

        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder tags(Tag... tags) {
            this.tags = tags;
            return this;
        }

        public Builder implementor(Object implementor) {
            this.implementor = implementor;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder configRootName(String configRootName) {
            this.configRootName = configRootName;
            return this;
        }

        public Builder registryType(MetricRegistry.Type scope) {
            this.registryType = scope;
            return this;
        }

        public MetricBuildItem build() {
            return new MetricBuildItem(metadata, implementor, enabled, configRootName, registryType, tags);
        }

    }
}
