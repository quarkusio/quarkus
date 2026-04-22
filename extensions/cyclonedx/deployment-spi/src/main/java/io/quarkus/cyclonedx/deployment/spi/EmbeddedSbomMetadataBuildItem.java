package io.quarkus.cyclonedx.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Produced by the CycloneDX build step after embedding a dependency SBOM,
 * carrying metadata about the embedded resource.
 */
public final class EmbeddedSbomMetadataBuildItem extends SimpleBuildItem {

    private final String resourceName;
    private final boolean compressed;

    public EmbeddedSbomMetadataBuildItem(String resourceName, boolean compressed) {
        this.resourceName = resourceName;
        this.compressed = compressed;
    }

    /**
     * The classpath resource name of the embedded SBOM.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Whether the embedded SBOM resource is GZIP-compressed.
     */
    public boolean isCompressed() {
        return compressed;
    }
}
