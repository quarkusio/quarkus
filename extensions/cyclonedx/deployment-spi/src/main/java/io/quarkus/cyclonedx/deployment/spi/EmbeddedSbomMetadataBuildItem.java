package io.quarkus.cyclonedx.deployment.spi;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Produced by the CycloneDX build step after embedding a dependency SBOM,
 * carrying metadata about the embedded resource.
 */
public final class EmbeddedSbomMetadataBuildItem extends SimpleBuildItem {

    private final String resourceName;
    private final Optional<Boolean> serveCompressed;

    public EmbeddedSbomMetadataBuildItem(String resourceName, Optional<Boolean> serveCompressed) {
        this.resourceName = resourceName;
        this.serveCompressed = serveCompressed;
    }

    /**
     * The classpath resource name of the embedded SBOM. The SBOM is always
     * stored uncompressed under this exact name.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * How the endpoint should apply GZIP compression when serving the embedded SBOM:
     * <ul>
     * <li>{@code Optional.of(true)} - always serve compressed;</li>
     * <li>{@code Optional.of(false)} - never serve compressed;</li>
     * <li>{@code Optional.empty()} - negotiate based on the request's {@code Accept-Encoding} header.</li>
     * </ul>
     * This does not affect how the SBOM is stored, which is always uncompressed.
     */
    public Optional<Boolean> getServeCompressed() {
        return serveCompressed;
    }
}
