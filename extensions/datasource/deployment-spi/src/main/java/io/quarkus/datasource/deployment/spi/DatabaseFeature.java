package io.quarkus.datasource.deployment.spi;

/**
 * Represents optional database features that may require specialized container images
 * or database configurations in development services.
 * <p>
 * Extensions can use {@link DataSourceFeatureRequirementBuildItem} to declare that
 * a datasource requires specific features, which influences automatic image selection in DevServices.
 */
public enum DatabaseFeature {
    /**
     * Vector search capabilities (e.g., pgvector for PostgreSQL).
     */
    VECTOR,

    /**
     * Spatial/geographic data support (e.g., PostGIS for PostgreSQL).
     */
    SPATIAL
}
